package de.sciss.mellite.tutorials

import java.awt.event.KeyEvent
import java.util.concurrent.TimeUnit

import de.sciss.file._
import de.sciss.lucre.artifact.{Artifact, ArtifactLocation}
import de.sciss.lucre.expr.{DoubleObj, LongObj}
import de.sciss.lucre.stm.store.BerkeleyDB
import de.sciss.mellite.{ActionOpenWorkspace, Mellite, Prefs}
import de.sciss.synth.io.AudioFile
import de.sciss.synth.proc.Implicits._
import de.sciss.synth.proc.{AudioCue, Workspace}

import scala.concurrent.Future
import scala.concurrent.duration.Duration

object Tutorial1PaulStretch extends Tutorial {
  def main(args: Array[String]): Unit =
    startMellite()

  val paradoxBase : File  = file("tut-paulstretch") / "src" / "main" / "paradox"
  val assets      : File  = paradoxBase / "assets" / "images"
  val assetPre            = "tut-paulstretch"
  val wsName              = "PaulStretchTutorial"
  val afName              = "337048_131348kaonayabell"

  def mkTempWorkspace(name: String): Workspace.Durable = {
    val config          = BerkeleyDB.Config()
    config.allowCreate  = true
    val folderP         = createTempDir()
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
      ws <- onEDT {
        mkTempWorkspace(wsName)
      }
      _ <- delay()
      wWorkspace <- onEDT {
        mainFrame.visible = false
        val w = findWindow(wsName)
        val pt0 = w.location
        //        val sz0 = w.size
        pt0.y = math.max(pt0.y, 64)
        resizeWindow(w, dx = -64, dy = -96)
        w.location = pt0
        w
      }
      _ <- delay()
      _ <- onEDT {
//        w.size      = sz0
        val b = findButtonByTT(windowComponent(wWorkspace), "Add Element")
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
      _ <- onEDT(clickMouse())
      _ <- delay()
      wNewFSc <- onEDT {
        val w = findWindowComponent("New FScape")
        val b = findButton(w, "OK")
        val pt = locBottomRight(b, inset = 8)
        moveMouse(pt)
        w
      }
      _ <- snapComponent(wNewFSc, s"$assetPre-new-fscape-name")
      _ <- onEDT(typeKey(KeyEvent.VK_ENTER))
      _ <- delay()
      _ <- onEDT {
        typeKey(KeyEvent.VK_UP)
        val b = findButtonByTT(windowComponent(wWorkspace), "View Selected Element")
        val pt = locBottomRight(b, inset = 8)
        moveMouse(pt)
//        pressMouse()
      }
      _ <- snapWindow(wWorkspace, s"$assetPre-fscape-in-folder")
      _ <- onEDT(clickMouse()) // releaseMouse())
      _ <- delay()
      wFScapeEditor <- onEDT {
        wWorkspace.visible = false
        val w = findWindow("FScape : FScape Graph Code")
        resizeWindow(w, dx = -128, dy = -256)
        w
      }
      _ <- selectMenu(wFScapeEditor, "Examples", "Plot Sine")
      _ <- onEDT(clickMouse())
      _ <- delay()
      _ <- selectMenu(wFScapeEditor, "Examples", "Plot Sine")
      _ <- snapWindow(wFScapeEditor, s"$assetPre-fscape-plot-sine-code")
      _ <- onEDT(typeKey(KeyEvent.VK_ESCAPE))
      _ <- delay()
      _ <- onEDT {
//        typeKey(KeyEvent.VK_ESCAPE)
        typeKey(KeyEvent.VK_DOWN)
        typeKey(KeyEvent.VK_DOWN)
        typeKey(KeyEvent.VK_END)
        typeKey(KeyEvent.VK_LEFT)
        for (_ <- 1 to 5) typeKey(KeyEvent.VK_BACK_SPACE)
      }
      _ <- delay()
      (ptLedX, ptLedY) <- onEDT {
        val pt = locTopRight(windowComponent(wFScapeEditor), inset = 8)
        pt.y // += 30  // XXX TODO tricky -- we want to hover over the error led of the stripe
        moveMouse(pt)
        (pt.x, pt.y)
      }
      _ <- delay()
      _ <- onEDT {
        moveMouse(ptLedX - 1, ptLedY + 30)  // XXX TODO tricky -- we want to hover over the error led of the stripe
      }
      _ <- snapWindow(wFScapeEditor, s"$assetPre-fscape-plot-sine-error")
      _ <- onEDT {
        pressKey  (KeyEvent.VK_CONTROL)
        typeKey   (KeyEvent.VK_Z) // undo
        typeKey   (KeyEvent.VK_S) // save / apply
        releaseKey(KeyEvent.VK_CONTROL)
      }
      _ <- delay(2000)  /// XXX TODO tricky wait for compilation
      _ <- onEDT {
        pressKey  (KeyEvent.VK_SHIFT)
        typeKey   (KeyEvent.VK_F10) // render
        releaseKey(KeyEvent.VK_SHIFT)
      }
      _ <- delay()
      _ <- onEDT(wFScapeEditor.visible = false)
      _ <- snapComponent(findWindowComponent("plot"), s"$assetPre-fscape-plot-sine-output")
      _ <- onEDT {
        wWorkspace.visible = true
        ws.cursor.step { implicit tx =>
          val f     = ws.root
          val loc   = ArtifactLocation.newVar(ArtifactLocation.newConst(paradoxBase.absolute))
          loc.name  = paradoxBase.base
          f.addLast(loc)
          val af    = paradoxBase / s"$afName.wav"
          val spec  = AudioFile.readSpec(af)
          val art   = Artifact(loc, af)
          val cue   = AudioCue.Obj(art, spec, LongObj.newVar(0L), DoubleObj.newVar(1.0))
          cue.name  =  af.base
          f.addLast(cue)
        }
        wWorkspace.front()
        closeWindowComponent("plot")
      }
      _ <- delay()
      _ <- onEDT {
        val tt = findTreeTable(windowComponent(wWorkspace))
        tt.requestFocus()
//        typeKey(KeyEvent.VK_TAB)
//        typeModKey(KeyEvent.VK_SHIFT, KeyEvent.VK_TAB)
        typeKey(KeyEvent.VK_DOWN)
        typeKey(KeyEvent.VK_DOWN)
      }
      _ <- snapWindow(wWorkspace, s"$assetPre-audio-file-in-folder")
      _ <- onEDT {
        val b = findButtonByTT(windowComponent(wWorkspace), "View Selected Element")
        val pt = locBottomRight(b, inset = 8)
        moveMouse(pt)
        clickMouse()
      }
      _ <- delay()
      wCue <- onEDT {
        wWorkspace.visible = false
        val w = findWindow(afName)
        resizeWindow(w, dx = -256, dy = -256)
        w
      }
      _ <- snapWindow(wCue, s"$assetPre-audio-file-view")
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
