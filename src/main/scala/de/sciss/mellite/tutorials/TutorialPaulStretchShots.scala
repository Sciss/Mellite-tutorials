/*
 *  Tutorial1PaulStretch.scala
 *  (Mellite-tutorials)
 *
 *  Copyright (c) 2019-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.mellite.tutorials

import java.awt.event.KeyEvent

import de.sciss.file._
import de.sciss.fscape.lucre.FScape
import de.sciss.lucre.artifact.{ArtifactLocation, Artifact => AArtifact}
import de.sciss.lucre.expr.{BooleanObj, DoubleObj, LongObj}
import de.sciss.synth.io.AudioFile
import de.sciss.synth.proc.Implicits._
import de.sciss.synth.proc.{AudioCue, Durable, Widget}

import scala.concurrent.Future

// This creates all screenshots of the tutorial
object TutorialPaulStretchShots extends Tutorial {
  def main(args: Array[String]): Unit =
    startMellite()

  val paradoxBase : File  = file("tut-paulstretch") / "src" / "main" / "paradox"
  val assets      : File  = paradoxBase / "assets" / "images"
  val assetPre            = "tut-paulstretch"
  val wsName              = "PaulStretchTutorial"
  val afName              = "337048_131348kaonayabell"
  def overwriteSnaps      = false

  type S = Durable

  def started(): Future[Unit] = {
    runShowFinalWidget()
  }

  def runShowFinalWidget(): Future[Unit] = {
    for {
      _ <- delay()
      _ <- onEDT {
        val ws = mkTempWorkspace(wsName)
        ws.cursor.step { implicit tx =>
          MkPaulStretchWorkspace.populate(ws)
          val wid = ws.root.head
          val fsc = wid.attr.$[FScape]("run").get
          val (loc, _, artIn) = mkInputArtifact()
          val artOut = AArtifact[S](loc, AArtifact.Child("bell-stretched.aif"))
          fsc.attr.put("in" , artIn)
          fsc.attr.put("out", artOut)
        }
      }
      _ <- delay()
      _ <- onEDT {
        typeKey(KeyEvent.VK_DOWN)
        typeModKey(KeyEvent.VK_CONTROL, KeyEvent.VK_ENTER)
      }
      _ <- delay()
      _ <- onEDT {
        findWindow(wsName).visible = false
      }
      _ <- snapWindow(findWindow(MkPaulStretchWorkspace.name), s"$assetPre-widget-ui-final")

    } yield ()
  }

  def mkInputArtifact()(implicit tx: S#Tx): (ArtifactLocation[S], AudioCue.Obj[S], AArtifact[S]) = {
    val loc   = ArtifactLocation.newVar[S](ArtifactLocation.newConst[S](paradoxBase.absolute))
    loc.name  = paradoxBase.base
    val af    = paradoxBase / s"$afName.wav"
    val spec  = AudioFile.readSpec(af)
    val art   = AArtifact[S](loc, af)
    val cue   = AudioCue.Obj[S](art, spec, LongObj.newVar[S](0L), DoubleObj.newVar[S](1.0))
    cue.name  =  af.base
    (loc, cue, art)
  }

  def runPopulateWorkspace(): Future[Unit] = {
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
      _ <- delay(2000) /// XXX TODO tricky wait for compilation
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
          val (loc, cue, _) = mkInputArtifact()
          f.addLast(loc)
          f.addLast(cue)
        }
        wWorkspace.front()
        closeWindowComponent("plot")
      }
      _ <- delay()
      ttRoot <- onEDT {
        val tt = findTreeTable(windowComponent(wWorkspace))
        tt.requestFocus()
//        typeKey(KeyEvent.VK_TAB)
//        typeModKey(KeyEvent.VK_SHIFT, KeyEvent.VK_TAB)
        typeKey(KeyEvent.VK_DOWN)
        typeKey(KeyEvent.VK_DOWN)
        tt
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
      ggBoot <- onEDT {
        mainFrame.visible = true
        val b = findButton(windowComponent(mainFrame), "Boot")
//        val pt = locBottomRight(b, inset = 8)
        b.doClick()
//        moveMouse(pt)
//        clickMouse()
        b
      }
      _ <- delay(2000)
      _ <- onEDT {
        val pt = locBottomRight(ggBoot, inset = 8)
        //        b.doClick()
        moveMouse(pt)
        wCue.front()
        typeKey(KeyEvent.VK_SPACE)
      }
      _ <- delay(200)
      _ <- onEDT {
        mainFrame.front()
        wCue.visible = false
      }
      _ <- snapWindow(mainFrame, s"$assetPre-audio-system-booted")
      wAttrFSc <- onEDT {
        wWorkspace.front()
        mainFrame.visible = false
        ws.cursor.step { implicit tx =>
          val f   = ws.root
          val fsc = f.head
          mkAttrMapFrame[S](ws, fsc)
        }
      }
      _ <- onEDT {
        val pt = wAttrFSc.window.location
        val sz = wAttrFSc.window.size
        val locWs = wWorkspace.location
        val szWs = wWorkspace.size
        pt.y = locWs.y + szWs.height + 4
        pt.x = locWs.x + (wWorkspace.size.width - sz.width) / 2
        wAttrFSc.window.location = pt
      }
      _ <- delay()
      (linkSrcX, linkSrcY, linkTgtX, linkTgtY) <- onEDT {
        val t = findTable(windowComponent(wAttrFSc.window)).peer
//        val tcm = t.peer.getColumnModel
        val ptTgt = t.getLocationOnScreen
//        val cx  = pt0.x + tcm.getColumn(0).getWidth + tcm.getColumn(1).getWidth/2
        val rTgt  = t.getCellRect(1, 1, false)
        val tgtX  = ptTgt.x + rTgt.x + rTgt.width/2
        val tgtY  = ptTgt.y + rTgt.y + rTgt.height - 2 // /2
        val tt    = ttRoot.peer
        val ptSrc = tt.getLocationOnScreen
        val rSrc  = tt.getCellRect(2, 0, false)
        val srcX  = ptSrc.x + rSrc.x + rSrc.width/2
        val srcY  = ptSrc.y + rSrc.y + rSrc.height/2

        moveMouse(srcX, srcY)
        pressMouse()

        (srcX, srcY, tgtX, tgtY)
      }
      _ <- delay(100)
      _ <- onEDT(moveMouse(linkTgtX, linkTgtY - 4))
      _ <- delay(250)
      //      _ <- onEDT(pressMouse())
      //      _ <- delay(250)
      _ <- onEDT {
        moveMouse(linkTgtX, linkTgtY)
      }
      _ <- snapWindow(wAttrFSc.window, s"$assetPre-dnd-to-attr", pointer = false, code = { g2 =>
        drawArrow(g2, linkSrcX, linkSrcY, linkTgtX, linkTgtY)
//        g2.drawLine(linkTgtX - 4, linkTgtY, linkTgtX + 4, linkTgtY)
//        g2.drawLine(linkTgtX, linkTgtY - 4, linkTgtX, linkTgtY + 4)
      })
      _ <- onEDT {
        typeKey(KeyEvent.VK_ESCAPE) // just to be sure; seems that drop doesn't work
        releaseMouse()
      }
      _ <- onEDT {
        wWorkspace.visible = false
        ws.cursor.step { implicit tx =>
          val f   = ws.root
          val fsc = f.head
          val cue = f.last
          fsc.attr.put("in", cue)
        }
      }
      _ <- snapWindow(wAttrFSc.window, s"$assetPre-link-in-to-attr")
      _ <- onEDT {
        ws.cursor.step { implicit tx =>
          val f   = ws.root
          val fsc = f.head
          val cue = f.last.asInstanceOf[AudioCue.Obj.Apply[S]]
          val loc = cue.artifact.location
          val artOut = AArtifact[S](loc, AArtifact.Child("normalized.aif"))
          fsc.attr.put("out", artOut)
        }
      }
      _ <- onEDT {
        val t = findTable(windowComponent(wAttrFSc.window)).peer
        val ptTgt = t.getLocationOnScreen
        val rTgt  = t.getCellRect(3, 1, false)
        val tgtX  = ptTgt.x + rTgt.x + rTgt.width/2
        val tgtY  = ptTgt.y + rTgt.y + rTgt.height/2
        moveMouse(tgtX, tgtY)
      }
      _ <- snapWindow(wAttrFSc.window, s"$assetPre-link-out-to-attr")
      _ <- onEDT {
        wWorkspace.front()
        wAttrFSc.window.visible = false
      }
      _ <- delay()
      _ <- onEDT {
        typeModKey(KeyEvent.VK_CONTROL, KeyEvent.VK_1)
      }
      _ <- delay()
      _ <- onEDT {
        "WIDGET".foreach(c => typeKey(c.toInt))
      }
      _ <- snapWindow(wWorkspace, s"$assetPre-type-new-widget")
      _ <- onEDT {
        typeKey(KeyEvent.VK_ENTER)
      }
      _ <- delay()
      _ <- onEDT {
        ttRoot.requestFocus()
        typeKey(KeyEvent.VK_DOWN)
      }
      _ <- snapWindow(wWorkspace, s"$assetPre-root-has-new-widget")
      wAttrWidget <- onEDT {
        ws.cursor.step { implicit tx =>
          val f = ws.root
          val fsc = f.head
          val wid = f.last
          wid.attr.put("run", fsc)
          mkAttrMapFrame[S](ws, wid)
        }
      }
      _ <- delay()
      _ <- onEDT {
        wWorkspace.visible = false
      }
      _ <- snapWindow(wAttrWidget.window, s"$assetPre-widget-attr")
      _ <- onEDT {
        ws.cursor.step { implicit tx =>
          val f = ws.root
          val wid = f.last.asInstanceOf[Widget[S]]
          wid.graph() = Widget.Graph {
            import de.sciss.lucre.expr.graph._
            import de.sciss.lucre.swing.graph._
            val r       = Runner("run")
            val in      = AudioFileIn()
            val out     = AudioFileOut()
            val render  = Button(" Render ")
            val cancel  = Button(" X ")

            in .value <--> Artifact("run:in")
            out.value <--> Artifact("run:out")

            val running = r.state sig_== 3
            render.clicked ---> r.run
            cancel.clicked ---> r.stop
            render.enabled = !running
            cancel.enabled = running

            val p = GridPanel(
              Label("Input:" ), in,
              Label("Output:"), out,
            )
            p.columns = 2
            p.border  = Border.Empty(8)
            p.hGap    = 8
            p.compact = true

            BorderPanel(
              north = p,
              south = FlowPanel(cancel, render),
            )
          }
          wid.attr.put("edit-mode", BooleanObj.newVar[S](false))
        }
        wWorkspace.front()
      }
      _ <- delay()
      _ <- onEDT {
        typeModKey(KeyEvent.VK_CONTROL, KeyEvent.VK_ENTER)
      }
      _ <- delay()
      _ <- onEDT {
        wAttrWidget.window.visible = false
        wWorkspace        .visible = false
      }
      _ <- snapWindow(findWindow("Widget"), s"$assetPre-widget-ui")
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
