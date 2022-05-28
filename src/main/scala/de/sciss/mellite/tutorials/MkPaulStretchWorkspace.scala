/*
 *  MkPaulStretchWorkspace.scala
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

import de.sciss.file._
import de.sciss.fscape.lucre.MacroImplicits.FScapeMacroOps
import de.sciss.lucre.{BooleanObj, Folder, Txn}
import de.sciss.lucre.store.BerkeleyDB
import de.sciss.proc.Implicits.ObjOps
import de.sciss.proc.MacroImplicits.WidgetMacroOps
import de.sciss.proc.{Durable, FScape, SoundProcesses, Widget, Workspace}

// This creates the downloadable final workspace inside `~/mellite/sessions/`
object MkPaulStretchWorkspace {
  def main(args: Array[String]): Unit = {
    val dir = userHome / "mellite" / "sessions"
    dir.mkdirs()
    run(dir / s"$name.mllt")
  }

  val name = "PaulStretch"

  type T = Durable.Txn

  def run(target: File): Unit = {
    SoundProcesses.init()
    FScape.init()
    Widget.init()

    require(!target.exists(), s"Workspace '${target.name}' already exists. Not overwriting.")
    val ds = BerkeleyDB.factory(target)
    val ws = Workspace.Durable.empty(target.toURI, ds)
    ws.cursor.step { implicit tx =>
      populate(ws)
      ws.dispose()
    }
  }

  def populate(ws: Workspace[T])(implicit tx: T): Unit = {
    val r = ws.root
    add(r)
  }

  def add[T <: Txn[T]](f: Folder[T])(implicit tx: T): Unit = {
    val m = this

    val fsc   = m.apply[T]()
    fsc.name  = m.name
    val w     = m.ui[T]()
    w.name    = m.name
    w.attr.put("run"       , fsc)
    w.attr.put("edit-mode" , BooleanObj.newVar(false))
    f.addLast(w)
  }

  def any2stringadd: Any = () // WTF Scala

  def apply[T <: Txn[T]]()(implicit tx: T): FScape[T] = {
    import de.sciss.fscape.lucre.graph.Ops._
    import de.sciss.fscape.lucre.graph._
    val f = FScape[T]()
    import de.sciss.fscape.graph.{AudioFileIn => _, AudioFileOut => _, _}
    f.setGraph {
      // version: 04-Jan-2020
      val in          = AudioFileIn("in")
      val winSizeSec  = "win-size-sec".attr(1.0)
      val stretch     = "stretch"     .attr(8.0).max(1.0)
      val fileType    = "out-type"    .attr(0)
      val smpFmt      = "out-format"  .attr(2)

      val N           = 4   // output window overlap
      val sr          = in.sampleRate
      val numFramesIn = in.numFrames
      val winSize     = (winSizeSec * sr).roundTo(1).toInt.max(1)
      val stepSizeOut = (winSize.toDouble / N).roundTo(1).toInt.max(1)
      val stepSizeIn  = (winSize / (N * stretch)).roundTo(1).toInt.max(1)
      val numStepsIn  = (numFramesIn.toDouble / stepSizeIn).ceil.toInt
      val numFramesOut= (numStepsIn - 1).max(0) * stepSizeOut + winSize
      val slidIn      = Sliding(in, size = winSize, step = stepSizeIn)
      val winAna      = GenWindow(winSize, shape = GenWindow.Hann)
      val inW         = slidIn * winAna
      val fftSize     = winSize.nextPowerOfTwo
      val fft         = Real1FFT(inW, winSize, padding = fftSize - winSize)
      val mag         = fft.complex.mag
      val phase       = WhiteNoise(math.Pi)
      val real        = mag * phase.cos
      val imag        = mag * phase.sin
      val rand        = real zip imag
      val ifft        = Real1IFFT(rand, fftSize)
      val slidOut     = ResizeWindow(ifft, fftSize, stop = winSize - fftSize)
      val winSyn      = GenWindow(winSize, shape = GenWindow.Hann)
      val outW        = slidOut * winSyn
      val lap         = OverlapAdd(outW, size = winSize, step = stepSizeOut)
      val lapTrim     = lap.take(numFramesOut)
      ProgressFrames(lapTrim, numFramesOut, "render")
      val maxAmp      = RunningMax(Reduce.max(lapTrim.abs)).last
      val gain        = 1.0 / maxAmp
      val norm        = BufferDisk(lapTrim) * gain
      val written     = AudioFileOut("out", norm,
        fileType = fileType, sampleFormat = smpFmt, sampleRate = sr)
      ProgressFrames(written, numFramesOut, "write")
    }
    f
  }

  def ui[T <: Txn[T]]()(implicit tx: T): Widget[T] = {
    import de.sciss.lucre.expr.ExImport._
    import de.sciss.lucre.expr.graph._
    import de.sciss.lucre.swing.graph._
    val w = Widget[T]()
    w.setGraph {
      // version: 04-Jan-2020
      val r       = Runner("run")
      val in      = AudioFileIn()
      val out     = AudioFileOut()
      val render  = Button(" Render ")
      val cancel  = Button(" X ")
      cancel.tooltip = "Cancel Rendering"
      val pb      = ProgressBar()

      in .value         <-> Artifact("run:in")
      out.value         <-> Artifact("run:out")
      out.fileType      <-> "run:out-type"   .attr(0)
      out.sampleFormat  <-> "run:out-format" .attr(2)

      val stretch = DoubleField()
      stretch.min = 1.0
      stretch.max = 1.0e18
      stretch.value <-> "run:stretch".attr(8.0)

      val winSize = DoubleField()
      winSize.unit= "sec"
      winSize.min = 0.01
      winSize.max = 100.0
      winSize.value <-> "run:win-size-sec".attr(1.0)

      val running = r.state sig_== 3
      render.clicked --> r.run
      cancel.clicked --> r.stop
      render.enabled  = !running
      cancel.enabled  = running
      pb.value        = (r.progress * 100).toInt

      val p = GridPanel(
        Label("Input:" ), in,
        Label("Output:"), out,
        Label(" "), Empty(),
        Label("Stretch Factor:" ), stretch,
        Label("Window Length:"  ), winSize,
      )
      p.columns = 2
      p.border  = Border.Empty(8)
      p.hGap    = 8
      p.compact = true

      BorderPanel(
        north = p,
        south = BorderPanel(
          center  = pb,
          east    = FlowPanel(cancel, render)
        )
      )
    }
    w
  }
}
