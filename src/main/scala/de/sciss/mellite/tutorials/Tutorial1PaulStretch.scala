package de.sciss.mellite.tutorials

import java.awt.event.InputEvent

import javax.swing.{JFrame, JMenu, JMenuBar, JMenuItem}

object Tutorial1PaulStretch extends Tutorial {
  def main(args: Array[String]): Unit = {
    startMellite()
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

  def started(): Unit = {
    // println("Main window: " + mainFrame)
    val mainC   = mainFrame.component.peer.asInstanceOf[JFrame] // asInstanceOf[scala.swing.Window]
    val mb      = mainC.getJMenuBar

    val mFile   = findMenu(mb, "File") // mb.contents.collectFirst { case mg: scala.swing.Menu if mg.text == "File" => mg } .get
    val ptFile  = mFile.getLocationOnScreen

    delay() {
      robot.mouseMove(ptFile.x + 4, ptFile.y + 4)
      robot.mousePress(InputEvent.BUTTON1_MASK)
      delay(100) {
        val mNew = findMenu(mFile, "New")
        val ptNew = mNew.getLocationOnScreen
        robot.mouseRelease(InputEvent.BUTTON1_MASK)
        robot.mouseMove(ptNew.x + 4, ptNew.y + 4)
//        robot.mousePress(InputEvent.BUTTON1_MASK)
        delay(500) {
          val mWorkspace  = findMenuItem(mNew, "Workspace...")
          val ptWorkspace = mWorkspace.getLocationOnScreen
          robot.mouseMove(ptWorkspace.x + 4, ptWorkspace.y + 4)
          delay(100) {
            snapWindow(mainFrame, "main-frame")
            sys.exit()
          }
        }
      }
//      val mNew        = JMenuOperator.findJMenu(mFile, "New", false, false)
//      val mNew        = JMenuItemOperator.findJMenuItem(mFile, "New", true, true)
//      println(s"mFile: $mFile")
//      println(s"mNew: $mNew")
//      val mWorkspace  = JMenuItemOperator.findJMenuItem(mNew, "Workspace...", true, true)
//      mWorkspace.setArmed(true)

//      sys.exit()
    }
  }
}
