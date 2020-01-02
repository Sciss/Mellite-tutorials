package de.sciss.mellite.tutorials

import java.util.concurrent.TimeUnit

import de.sciss.file._
import de.sciss.lucre.stm.store.BerkeleyDB
import de.sciss.mellite.{ActionOpenWorkspace, Mellite, Prefs}
import de.sciss.synth.proc.Workspace

import scala.concurrent.Future
import scala.concurrent.duration.Duration

object Tutorial1PaulStretch extends Tutorial {
  def main(args: Array[String]): Unit =
    startMellite()

  val assets: File  = file("tut-paulstretch") / "src" / "main" / "paradox" / "assets" / "images"
  val assetPre      = "tut-paulstretch"
  val wsName        = "PaulStretchTutorial"

  def mkTempWorkspace(name: String): Workspace.Durable = {
    val config          = BerkeleyDB.Config()
    config.allowCreate  = true
    val folderP         = File.createTemp(directory = true)
    val folder          = folderP / s"$name.mllt"
    val ds              = BerkeleyDB.factory(folder, config)
    config.lockTimeout  = Duration(Prefs.dbLockTimeout.getOrElse(Prefs.defaultDbLockTimeout), TimeUnit.MILLISECONDS)
    val w               = Workspace.Durable.empty(folder, ds)
    val u               = Mellite.mkUniverse(w)
    ActionOpenWorkspace.openGUI(u)
    w
  }

  def started(): Future[Unit] = {
    for {
      _ <- delay()
      _ <- onEDT {
        mkTempWorkspace(wsName)
      }
      _ <- delay()
      _ <- onEDT {
        mainFrame.visible = false
        val w = findWindow(wsName)
        val pt0 = w.location
        pt0.y = math.max(pt0.y, 64)
        w.location = pt0
        val b = findButtonByTT(w.component.contents.head, "Add Element")
//        val pt = locBottomRight(b, 8)
//        moveMouse(pt)
        b.doClick()
      }
      _ <- delay()
      _ <- onEDT {
//        val w = findPopup()
        selectPopup("Composition", "FScape")
//        println(w)
      }
      _ <- snapComponent(findPopup(), s"$assetPre-popup-folder-new-object")

//      _ <- selectMenu(mainFrame, "File", "New", "Workspace...")
    } yield ()
  }

  def runNewWorkspace(): Future[Unit] = {
    for {
      _ <- delay()
      _ <- selectMenu(mainFrame, "File", "New", "Workspace...")
      _ <- snapWindow(mainFrame, s"$assetPre-menu-new-workspace")
//      _ <- ensureEDT { typeKey(KeyEvent.VK_ESCAPE) }
      _ <- onEDT {
        clickMouse()
        mainFrame.visible = false
      }
      _ <- delay()
      wNewWorkspace <- onEDT {
        val w   = findWindowComponent("New Workspace")
        val b   = findButton(w, "Durable")
        val pt  = locBottomRight(b, inset = 8)
        moveMouse(pt)
        w
      }
      _ <- snapComponent(wNewWorkspace, s"$assetPre-dlg-new-workspace")
      _ <- onEDT(clickMouse())
      _ <- delay()
      wLocNewWorkspace <- onEDT {
        val w     = findWindowComponent("Location for New Workspace")
        val fc    = findFileChooser(w)
        fc.selectedFile = userHome / "Documents" / wsName //  "mellite" / "sessions" / "MyWorkspace"
        val tf    = findTextField(fc, wsName)
        tf.requestFocus()
        w
      }
      _ <- snapComponent(wLocNewWorkspace, s"$assetPre-filechooser-new-workspace", pointer = false)

    } yield ()
  }
}
