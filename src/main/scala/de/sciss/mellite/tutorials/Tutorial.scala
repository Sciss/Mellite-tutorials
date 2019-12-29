package de.sciss.mellite.tutorials

import java.awt.event.ActionListener
import java.awt.image.BufferedImage
import java.awt.{Color, GraphicsConfiguration, GraphicsDevice, GraphicsEnvironment, Point, Rectangle, Robot, Toolkit}

import de.sciss.desktop.Window
import de.sciss.file._
import de.sciss.mellite.{LogFrame, MainFrame, Mellite}
import javax.imageio.ImageIO
import javax.swing.Timer

import scala.annotation.tailrec
import scala.swing.Swing

trait Tutorial {
  def started(): Unit

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

  def mainFrame: MainFrame = {
    require (_mainFrame != null)
    _mainFrame
  }

  def logFrame: LogFrame = {
    require (_logFrame != null)
    _logFrame
  }

  def defaultDelay: Int = 1000

  def delay(millis: Int = defaultDelay)(action: => Unit): Unit = {
    _timer.setInitialDelay(millis)
    lazy val a: ActionListener = Swing.ActionListener { _ =>
      _timer.removeActionListener(a)
      action
    }
    _timer.addActionListener(a)
    _timer.restart()
  }

  def snapWindow(w: Window, name: String): Unit = {
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
        started()
//        sys.exit()
      }
    }
  }
}
