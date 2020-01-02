package de.sciss.mellite.tutorials

import java.awt.event.{ActionListener, ComponentAdapter, ComponentEvent, ComponentListener, HierarchyEvent, HierarchyListener, InputEvent}
import java.awt.image.BufferedImage
import java.awt.{Color, EventQueue, GraphicsConfiguration, GraphicsDevice, GraphicsEnvironment, Image, MouseInfo, Point, Rectangle, Robot}

import de.sciss.desktop.Window
import de.sciss.file._
import de.sciss.mellite.{LogFrame, MainFrame, Mellite}
import de.sciss.treetable.j.TreeTable
import javax.imageio.ImageIO
import javax.swing.event.{ChangeEvent, ChangeListener}
import javax.swing.{JDialog, JFrame, JMenu, JMenuBar, JMenuItem, JPopupMenu, JWindow, MenuElement, Timer}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.swing.{Button, Component, Swing, TextField, UIElement}
import scala.util.control.NonFatal

trait Tutorial {
  def started(): Future[Unit]

  def assets: File

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

  def onEDT[A](body: => A): Future[A] = {
    val res = Promise[A]()

    def run(): Unit =
      try {
        val a = body
        // println("---10 ensureEDT")
        res.trySuccess(a)
      } catch {
        case NonFatal(ex) =>
          println("---11")
          ex.printStackTrace()
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

  def moveMouse(x: Int, y: Int): Unit = {
    requireEDT()
    robot.mouseMove(x, y)
  }

  def moveMouse(pt: Point): Unit = {
    requireEDT()
    robot.mouseMove(pt.x, pt.y)
  }

  def pressMouse(): Unit = {
    requireEDT()
    robot.mousePress(InputEvent.BUTTON1_MASK)
  }

  def releaseMouse(): Unit = {
    requireEDT()
    robot.mouseRelease(InputEvent.BUTTON1_MASK)
  }

  def clickMouse(): Unit = {
    pressMouse()
    releaseMouse()
  }

  def createTempDir(): File = {
    val res = File.createTemp(directory = true)
    sys.addShutdownHook {
      def deleteRecursively(f: File): Unit = {
        if (f.isDirectory) Option(f.listFiles).getOrElse(Array.empty).foreach(deleteRecursively)
        if (!f.delete()) f.deleteOnExit()
      }
      deleteRecursively(res)
    }
    res
  }

  def pressKey(code: Int): Unit = {
    requireEDT()
    robot.keyPress  (code)
  }

  def releaseKey(code: Int): Unit = {
    requireEDT()
    robot.keyRelease(code)
  }

  def typeKey(code: Int): Unit = {
    requireEDT()
    robot.keyPress  (code)
    robot.keyRelease(code)
  }

  def typeModKey(modifier: Int, code: Int): Unit = {
    requireEDT()
    robot.keyPress  (modifier)
    robot.keyPress  (code)
    robot.keyRelease(code)
    robot.keyRelease(modifier)
  }

  implicit val executionContext: ExecutionContext = ExecutionContext.global

  def mapEDT[A, B](in: Future[A])(body: A => B): Future[B] =
    in.flatMap { a =>
      onEDT(body(a))
    }

  def flatMapEDT[A, B](in: Future[A])(body: A => Future[B]): Future[B] =
    in.flatMap { a =>
      ensureFlatEDT(body(a))
    }

  def selectMenu(window: Window, path0: String, path: String*): Future[Unit] =
    ensureFlatEDT(selectMenuEDT(window, path0, path: _*))

  def selectPopup(path0: String, path: String*): Future[Unit] =
    ensureFlatEDT {
      val p = findPopup()
      selectPopupMenuEDT(p.peer, path0, path: _*)
    }

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

  private def moveOverMenu(m1: JMenuItem, press: Boolean): Future[Unit] = try {
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
      val sz = m1.getSize()
      val wh = sz.width   >> 1
      val hh = sz.height   >> 1

      // println(s"Waiting again: ${m1.getText} -- $press")
      val pt = m1.getLocationOnScreen
      moveMouse (pt.x + wh, pt.y + hh)
      if (press) {
        pressMouse()
      }

      res1.future
    }
  } catch {
    case NonFatal(ex) =>
      println("---5")
      Future.failed(ex)
  }

  private def waitForMenu(m: JMenuItem, press: Boolean = false): Future[Unit] = try {
    if (m.isShowing) {
//       println(s"Showing: ${m.getText}")
      moveOverMenu(m, press = press)
    } else {
//       println(s"Waiting: ${m.getText}")
      val res1 = Promise[Unit]()

      def complete(): Unit = {
//         println(s"Shown: ${m.getText}")
        m.removeComponentListener(cl)
        m.removeHierarchyListener(hl)
        val fut = moveOverMenu(m, press = press)
//         println("---1")
        res1.tryCompleteWith(fut)
      }

      lazy val cl: ComponentListener = new ComponentAdapter {
        override def componentShown(e: ComponentEvent): Unit = complete()
      }

      lazy val hl: HierarchyListener = (_: HierarchyEvent) => if (m.isShowing) complete()
      m.addComponentListener(cl)
      m.addHierarchyListener(hl)
      res1.future
    }
  } catch {
    case NonFatal(ex) =>
      println("---6")
      Future.failed(ex)
  }

  private def loopShowMenu(futP: Future[Unit], parent: MenuElement, rem: List[String]): Future[Unit] = rem match {
    case Nil => futP

    case head :: tail =>
      findMenuItem(parent, head) match {
        case m1: JMenu =>
          flatMapEDT(futP) { _ =>
            val w1 = waitForMenu(m1)
            loopShowMenu(w1, m1, tail)
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

  private def selectMenuEDT(window: Window, path0: String, path: String*): Future[Unit] = try {
    val mainC   = window.component.peer.asInstanceOf[JFrame]
    val mb      = mainC.getJMenuBar
    val m0      = findMenu(mb, path0)
    val fut0 = waitForMenu(m0, press = true)

    loopShowMenu(fut0, m0, path.toList)

  } catch {
    case NonFatal(ex) =>
      println("---8")
      Future.failed(ex)
  }

  private def selectPopupMenuEDT(m0: JPopupMenu, path0: String, path: String*): Future[Unit] = try {
    loopShowMenu(Future.successful(), m0, path0 :: path.toList)

  } catch {
    case NonFatal(ex) =>
      println("---8b")
      ex.printStackTrace()
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

  def findMenuItem(m: MenuElement, name: String): JMenuItem = {
    val c = m.getSubElements
//    println(s"FIND $name")
//    println(c.toList.map(_.getClass).mkString("\n"))
    val opt = c.iterator.map(_.getComponent).collectFirst {
      case it: JMenuItem if { /*println(it.getText);*/ it.getText == name } => it
      case it: JPopupMenu => findMenuItem(it, name)
    }
    opt.getOrElse(sys.error(s"Not found: $name"))

//    val n = m.getItemCount
//    var i = 0
//    while (i < n) {
//      val c = m.getItem(i)
//      if (c.getText == name) return c
//      i += 1
//    }
//    sys.error(s"Not found: $name")
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

  def snapWindow(w: => Window, name: String, pointer: Boolean = true): Future[Unit] =
    delay(repaintDelay).flatMap( _ =>
      ensureFlatEDT(snapComponentEDT(w.component, name, pointer = pointer))
    )

  def snapComponent(c: => Component, name: String, pointer: Boolean = true): Future[Unit] =
    delay(repaintDelay).flatMap( _ =>
      ensureFlatEDT(snapComponentEDT(c, name, pointer = pointer))
    )

  private[this] lazy val pointerImg: Image = {
    val url = getClass.getResource("/GnomePointer.png")
//    Toolkit.getDefaultToolkit.getImage(url)
    ImageIO.read(url)
    // Toolkit.getDefaultToolkit.createImage(url)
  }

  private def snapComponentEDT(c: UIElement, name: String, pointer: Boolean): Future[Unit] = try {
    requireEDT()
    val img = robot.createScreenCapture(graphicsConfiguration.getBounds)
    if (pointer) {
      val pi = MouseInfo.getPointerInfo
      val pm = pi.getLocation
      val gImg = img.createGraphics()
//      println(s"pointerImg = $pointerImg")
      gImg.drawImage(pointerImg, pm.x - 1, pm.y - 1, null)  // hot-spot: (1, 1)
      gImg.dispose()
    }

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

    ImageIO.write(crop, "png", assets / s"$name.png")
    crop.flush()
    // println("---2 snapWindowEDT")
    Future.successful(())
  } catch {
    case NonFatal(ex) =>
      println("---4 snapWindowEDT")
      Future.failed(ex)
  }

  def findPopup(): scala.swing.PopupMenu = {
    val opt = java.awt.Window.getWindows.collectFirst {
      case w: JWindow if w.getType == java.awt.Window.Type.POPUP => w
    } .flatMap { w =>
      findChild(w.getContentPane) {
          case j: javax.swing.JPopupMenu => j
        }
    }
    val j = opt.getOrElse(sys.error("No popup menu found"))
    new scala.swing.PopupMenu {
      override lazy val peer: JPopupMenu = j
    }
  }

  def windowComponent(w: Window): Component = {
    requireEDT()
    w.component.contents.head
  }

  def resizeWindow(w: Window, dx: Int = 0, dy: Int = 0): Unit = {
    requireEDT()
    w.component.peer match {
      case jf: JFrame => jf.setSize(jf.getWidth + dx, jf.getHeight + dy)
      case _ =>
        println("Warning: could not resize window")
    }
  }

  def findWindow(title: String): Window = {
    onEDT()
    val opt = Mellite.windowHandler.windows.find(_.title == title)
    val w = opt.getOrElse(sys.error(s"Window '$title' not found'"))
    w
  }

  def closeWindowComponent(title: String): Unit = {
    val opt = java.awt.Window.getWindows.collectFirst {
      case f: JFrame  if f.getTitle == title => f.dispose(); ()
      case d: JDialog if d.getTitle == title => d.dispose(); ()
    }
    opt.getOrElse(sys.error(s"Window '$title' not found'"))
  }

  def findWindowComponent(title: String): Component = {
    onEDT()
    val opt = java.awt.Window.getWindows.collectFirst {
      case f: JFrame  if f.getTitle == title => f.getRootPane
      case d: JDialog if d.getTitle == title => d.getRootPane
    }
    val w = opt.getOrElse(sys.error(s"Window '$title' not found'"))
    Component.wrap(w)
  }

  def locBottomRight(c: Component, inset: Int = 0): Point = {
    onEDT()
    val pt = c.locationOnScreen
    val sz = c.size
    new Point(pt.x + sz.width - inset, pt.y + sz.height - inset)
  }

  def locTopRight(c: Component, inset: Int = 0): Point = {
    onEDT()
    val pt = c.locationOnScreen
    val sz = c.size
    new Point(pt.x + sz.width - inset, pt.y + inset)
  }

  def findTreeTable(parent: Component): Component = {
    onEDT()
    val res = findChild(parent.peer) {
      case a: TreeTable => Component.wrap(a)
    }
    res.getOrElse(sys.error("Tree table not found"))
  }

  def findButton(parent: Component, text: String): Button = {
    onEDT()
    val res = findChild(parent.peer) {
      case a: javax.swing.JButton if a.getText == text =>
        new scala.swing.Button {
          override lazy val peer: javax.swing.JButton = a
        }
    }
    res.getOrElse(sys.error(s"Button '$text' not found"))
  }

  def findButtonByTT(parent: Component, tt: String): Button = {
    onEDT()
    val res = findChild(parent.peer) {
      case a: javax.swing.JButton if a.getToolTipText == tt =>
        new scala.swing.Button {
          override lazy val peer: javax.swing.JButton = a
        }
    }
    res.getOrElse(sys.error(s"Button with tool-tip '$tt' not found"))
  }

  def findTextField(parent: Component, text: String): TextField = {
    onEDT()
    val res = findChild(parent.peer) {
      case a: javax.swing.JTextField if a.getText == text =>
        new scala.swing.TextField {
          override lazy val peer: javax.swing.JTextField = a
        }
    }
    res.getOrElse(sys.error(s"Text field '$text' not found"))
  }

  def findFileChooser(parent: Component): scala.swing.FileChooser = {
    onEDT()
    val res = findChild(parent.peer) {
      case a: javax.swing.JFileChooser =>
        new scala.swing.FileChooser { override lazy val peer: javax.swing.JFileChooser = a }
    }
    res.getOrElse(sys.error("File chooser not found"))
  }

  private def findChild[A](parent: java.awt.Container)(test: PartialFunction[java.awt.Container, A]): Option[A] = {
    if (test.isDefinedAt(parent)) Some(test(parent)) // Component.wrap(p))
    else {
      var ci = 0
      while (ci < parent.getComponentCount) {
        parent.getComponent(ci) match {
          case jc: javax.swing.JComponent =>
            val opt = findChild(jc)(test)
            if (opt.isDefined) return opt
          case _ =>
        }
        ci += 1
      }
      None
    }
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
//          quit()
        }
      }
    }
  }
}
