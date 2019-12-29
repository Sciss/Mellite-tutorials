package de.sciss.mellite.tutorials

import java.awt.event.{ActionListener, ComponentAdapter, ComponentEvent, ComponentListener, HierarchyEvent, HierarchyListener, InputEvent}
import java.awt.image.BufferedImage
import java.awt.{Color, EventQueue, GraphicsConfiguration, GraphicsDevice, GraphicsEnvironment, Point, Rectangle, Robot}

import de.sciss.desktop.Window
import de.sciss.file._
import de.sciss.mellite.{LogFrame, MainFrame, Mellite}
import javax.imageio.ImageIO
import javax.swing.event.{ChangeEvent, ChangeListener}
import javax.swing.{JFrame, JMenu, JMenuBar, JMenuItem, Timer}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.swing.Swing
import scala.util.control.NonFatal

trait Tutorial {
  def started(): Future[Unit]

//  def mainWindow: Window = Mellite.windowHandler.mainWindow

  private[this] var _logFrame   : LogFrame  = _
  private[this] var _mainFrame  : MainFrame = _
  private[this] lazy val _screenDevice: GraphicsDevice = {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment
    ge.getDefaultScreenDevice
  }
  private[this] lazy val _graphicsConfiguration: GraphicsConfiguration =
    screenDevice.getDefaultConfiguration

  private[this] lazy val _robot: Robot = new java.awt.Robot(screenDevice)

  private[this] lazy val _timer: Timer = {
    val res = new Timer(1000, null)
    res.setRepeats(false)
    res
  }

  def screenDevice          : GraphicsDevice        = _screenDevice
  def graphicsConfiguration : GraphicsConfiguration = _graphicsConfiguration

  def robot: Robot = _robot

  def isEDT: Boolean = EventQueue.isDispatchThread

  def requireEDT(): Unit = require(isEDT)

  def ensureEDT[A](body: => A): Future[A] = {
    val res = Promise[A]()

    def run(): Unit =
      try {
        val a = body
        // println("---10 ensureEDT")
        res.trySuccess(a)
      } catch {
        case NonFatal(ex) =>
          println("---11")
          res.tryFailure(ex)
      }

    if (isEDT) run() else Swing.onEDT(run())
    res.future
  }

  def ensureFlatEDT[A](body: => Future[A]): Future[A] = {
    val res = Promise[A]()
    def run(): Unit =
      try {
        val a = body
        res.tryCompleteWith(a)
      } catch {
        case NonFatal(ex) =>
          res.tryFailure(ex)
      }

    if (isEDT) run() else {
      Swing.onEDT(run())
    }
    res.future
  }

  def moveMouse(x: Int, y: Int): Unit =
    robot.mouseMove(x, y)

  def pressMouse(x: Int, y: Int): Unit = {
    robot.mouseMove(x, y)
    robot.mousePress(InputEvent.BUTTON1_MASK)
  }

  def releaseMouse(): Unit =
    robot.mouseRelease(InputEvent.BUTTON1_MASK)

  implicit val executionContext: ExecutionContext = ExecutionContext.global

  def mapEDT[A, B](in: Future[A])(body: A => B): Future[B] =
    in.flatMap { a =>
      ensureEDT(body(a))
    }

  def flatMapEDT[A, B](in: Future[A])(body: A => Future[B]): Future[B] =
    in.flatMap { a =>
      ensureFlatEDT(body(a))
    }

  def selectMenu(window: Window, path0: String, path: String*): Future[Unit] =
    ensureFlatEDT(selectMenuEDT(window, path0, path: _*))

  private[this] val keepGoing: Thread = new Thread("wait-for-quit") {
    override def run(): Unit = {
      keepGoing.synchronized {
        keepGoing.wait()
      }
      sys.exit()
    }

    start()
  }

  def quit(): Unit = keepGoing.synchronized { keepGoing.notify() }

  private def selectMenuEDT(window: Window, path0: String, path: String*): Future[Unit] = try {
    val mainC   = window.component.peer.asInstanceOf[JFrame]
    val mb      = mainC.getJMenuBar
    val m0      = findMenu(mb, path0)

    def moveOverMenu(m1: JMenuItem, press: Boolean): Future[Unit] = try {
      if (m1.isSelected) {
        // println("---3")
        Future.successful(())
      }
      else {
        val res1 = Promise[Unit]()
        val isMenu = m1 match {
          case _: JMenu => true
          case _        => false
        }

        def testComplete(): Unit =
          if (m1.isSelected || (!isMenu && m1.isArmed)) {
            m1.removeChangeListener(cl)
            if (press) releaseMouse()
            // println("---12")
            res1.trySuccess(())
          }

        lazy val cl: ChangeListener = (_: ChangeEvent) => {
          //println(s"${m1.getText}.isSelected? ${m1.isSelected}; isArmed? ${m1.isArmed}")
          testComplete()
        }

        m1.addChangeListener(cl)

        // println(s"Waiting again: ${m1.getText} -- $press")
        val pt = m1.getLocationOnScreen
        if (press)
          pressMouse(pt.x + 4, pt.y + 4)
        else
          moveMouse (pt.x + 4, pt.y + 4)

        res1.future
      }
    } catch {
      case NonFatal(ex) =>
        println("---5")
        Future.failed(ex)
    }

    def waitForMenu(m: JMenuItem, press: Boolean = false): Future[Unit] = try {
      if (m.isShowing) {
        // println(s"Showing: ${m.getText}")
        moveOverMenu(m, press = press)
      } else {
        // println(s"Waiting: ${m.getText}")
        val res1 = Promise[Unit]()

        def complete(): Unit = {
          // println(s"Shown: ${m.getText}")
          m.removeComponentListener(cl)
          m.removeHierarchyListener(hl)
          val fut = moveOverMenu(m, press = press)
          // println("---1")
          res1.tryCompleteWith(fut)
        }

        lazy val cl: ComponentListener = new ComponentAdapter {
          override def componentShown(e: ComponentEvent): Unit = {
            complete()
          }
        }
//        m.addAncestorListener(new AncestorListener {
//          def ancestorAdded(event: AncestorEvent): Unit =
//            println(s"ancestorAdded -- ${m.getText} -- ${m.isShowing}")
//
//          def ancestorRemoved(event: AncestorEvent): Unit = ()
//
//          def ancestorMoved(event: AncestorEvent): Unit = ()
//        })
//        m.addContainerListener(new ContainerListener {
//          def componentAdded(e: ContainerEvent): Unit =
//            println(s"componentAdded -- ${m.getText} -- ${m.isShowing}")
//
//            def componentRemoved(e: ContainerEvent): Unit = ()
//        })
        lazy val hl: HierarchyListener = new HierarchyListener {
          def hierarchyChanged(e: HierarchyEvent): Unit =
            if (m.isShowing) complete()
//            println(s"hierarchyChanged -- ${m.getText} -- ${m.isShowing}")
        }
        m.addComponentListener(cl)
        m.addHierarchyListener(hl)
        res1.future
      }
    } catch {
      case NonFatal(ex) =>
        println("---6")
        Future.failed(ex)
    }

    val fut0 = waitForMenu(m0, press = true)

    def loop(futP: Future[Unit], parent: JMenu, rem: List[String]): Future[Unit] = rem match {
      case Nil =>
        futP

      case head :: tail =>
        findMenuItem(parent, head) match {
          case m1: JMenu =>
            flatMapEDT(futP) { _ =>
              val w1 = waitForMenu(m1)
              loop(w1, m1, tail)
            }

          case m1: JMenuItem if tail.isEmpty =>
            flatMapEDT(futP) { _ =>
              waitForMenu(m1)
            }

          case _ =>
            println("---7")
            Future.failed(new Exception("Leaf (menu item) while remaining path components"))
        }

    }

    loop(fut0, m0, path.toList)

  } catch {
    case NonFatal(ex) =>
      println("---8")
      Future.failed(ex)
  }

  def findMenu(mb: JMenuBar, name: String): JMenu = {
    val n = mb.getMenuCount
    var i = 0
    while (i < n) {
      val m = mb.getMenu(i)
      if (m.getText == name) return m
      i += 1
    }
    sys.error(s"Not found: $name")
  }

  def findMenu(m: JMenu, name: String): JMenu = {
    val n = m.getItemCount
    var i = 0
    while (i < n) {
      m.getItem(i) match {
        case c: JMenu if c.getText == name => return c
        case _ =>
      }
      i += 1
    }
    sys.error(s"Not found: $name")
  }

  def findMenuItem(m: JMenu, name: String): JMenuItem = {
    val n = m.getItemCount
    var i = 0
    while (i < n) {
      val c = m.getItem(i)
      if (c.getText == name) return c
      i += 1
    }
    sys.error(s"Not found: $name")
  }

  def mainFrame: MainFrame = {
    require (_mainFrame != null)
    _mainFrame
  }

  def logFrame: LogFrame = {
    require (_logFrame != null)
    _logFrame
  }

  def defaultDelay: Int = 1000

  def repaintDelay: Int = 1000

  def delay(millis: Int = defaultDelay): Future[Unit] =
    ensureFlatEDT(delayEDT(millis = millis))

  private def delayEDT(millis: Int = defaultDelay): Future[Unit] = {
    val res = Promise[Unit]()
    _timer.setInitialDelay(millis)
    lazy val a: ActionListener = Swing.ActionListener { _ =>
      _timer.removeActionListener(a)
      // println("---9 (delay)")
      res.trySuccess(())
    }
    _timer.addActionListener(a)
    _timer.restart()
    res.future
  }

  def snapWindow(w: Window, name: String): Future[Unit] =
    delay(repaintDelay).flatMap( _ =>
      ensureFlatEDT(snapWindowEDT(w, name))
    )

  private def snapWindowEDT(w: Window, name: String): Future[Unit] = try {
    requireEDT()
    val img = robot.createScreenCapture(graphicsConfiguration.getBounds)
    val c   = w.component
    val pt  = c.locationOnScreen
    val sz  = c.size
//    val g   = img.createGraphics()
//    g.setColor(Color.red)
//    g.drawRect(pt.x, pt.y, sz.width, sz.height)
//    g.drawLine(pt.x - 4, pt.y, pt.x + 4, pt.y)
//    g.drawLine(pt.x, pt.y - 4, pt.x, pt.y + 4)
//    g.dispose()
    val r   = new Rectangle(pt, sz)

    def isWhite(x: Int, y: Int): Boolean =
      (img.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF

    @tailrec
    def checkLeft(moved: Boolean = false): Boolean = {
      val white = (r.y until (r.y + r.height)).forall { y =>
        isWhite(r.x, y)
      }
      if (white || r.x <= 0) moved
      else {
        r.x -= 1; r.width += 1; checkLeft(moved = true)
      }
    }

    @tailrec
    def checkTop(moved: Boolean = false): Boolean = {
      val white = (r.x until (r.x + r.width)).forall { x =>
        isWhite(x, r.y)
      }
      if (white || r.y <= 0) moved
      else {
        r.y -= 1; r.height += 1; checkTop(moved = true)
      }
    }

    @tailrec
    def checkRight(moved: Boolean = false): Boolean = {
      val white = (r.y until (r.y + r.height)).forall { y =>
        isWhite(r.x + r.width - 1, y)
      }
      if (white || r.x + r.width >= img.getWidth) moved
      else {
        r.width += 1; checkRight(moved = true)
      }
    }

    @tailrec
    def checkBottom(moved: Boolean = false): Boolean = {
      val white = (r.x until (r.x + r.width)).forall { x =>
        isWhite(x, r.y + r.height - 1)
      }
      if (white || r.y + r.height >= img.getHeight) moved
      else {
        r.height += 1; checkBottom(moved = true)
      }
    }

    @tailrec
    def findCrop(): Unit =
      if (checkLeft() | checkTop() | checkRight() | checkBottom()) findCrop()

    findCrop()

    val crop = new BufferedImage(r.width, r.height, img.getType)
    val g    = crop.createGraphics()
    g.drawImage(img, -r.x, -r.y, null)
    g.dispose()
    img.flush()

    ImageIO.write(crop, "png", file(s"/data/temp/$name.png"))
    crop.flush()
    // println("---2 snapWindowEDT")
    Future.successful(())
  } catch {
    case NonFatal(ex) =>
      println("---4 snapWindowEDT")
      Future.failed(ex)
  }

  def startMellite(): Unit = {
    Mellite.main(Array("--no-log-frame"))
    Swing.onEDT {
      val gc    = screenDevice.getDefaultConfiguration
      val blank = new java.awt.Window(null, gc)
      blank.setBounds(gc.getBounds)
      blank.setBackground(Color.white)
      blank.setVisible(true)
      blank.toBack()

      val w0 = Mellite.windowHandler.windows.toList
      w0 match {
        case (lf: LogFrame) :: (mf: MainFrame) :: Nil =>
          _logFrame   = lf
          _mainFrame  = mf
        case (mf: MainFrame) :: Nil =>
          _mainFrame  = mf
        case _ => sys.error(w0.toString)
      }

      mainFrame.location = new Point(32, 64)
      Swing.onEDT {
        val fut = started()
        fut.onComplete { tr =>
          println(s"Result: $tr")
          quit()
        }
      }
    }
  }
}
