/*
 *  Resampling.scala
 *  (Data-to-Process)
 *
 *  Copyright (c) 2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v2+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.datatoprocess

import java.awt.image.DataBuffer
import java.util.Locale
import javax.imageio.ImageIO

import de.sciss.file._
import de.sciss.fscape.graph.ImageFile
import de.sciss.fscape.gui.SimpleGUI
import de.sciss.fscape.{Graph, graph, stream}
import scopt.OptionParser

import scala.swing.Swing

object Resampling {
  case class Config(templateIn    : File    = file("in-%d.jpg"),
                    templateOut   : File    = file("out-%d.jpg"),
                    idxRange0     : Range.Inclusive = 1 to 500,
                    factor        : Double  = 2.0,
                    zeroCrossings : Int     = 15,
                    kaiserBeta    : Double  = 7.5,
                    rollOff       : Double  = 0.86
                   )

  def main(args: Array[String]): Unit = {
    val default = Config()
    val parser = new OptionParser[Config]("Resampling") {
      opt[File  ]('i', "template-in")  required() text "Image file template input"  action { (x, c) => c.copy(templateIn  = x) }
      opt[File  ]('o', "template-out") required() text "Image file template output" action { (x, c) => c.copy(templateOut = x) }
      opt[Int   ]("start")             text s"Start frame (inclusive; default: ${default.idxRange0.head})" action { (x, c) =>
        c.copy(idxRange0 = x to c.idxRange0.end)
      }
      opt[Int   ]("end")              text s"End frame (inclusive; default: ${default.idxRange0.last})" action { (x, c) =>
        c.copy(idxRange0 = c.idxRange0.start to x)
      }
      opt[Double]('f', "factor")
        .text(s"Resampling factor (higher is slower; default: ${default.factor})")
        .action { (x, c) => c.copy(factor = x) }
      opt[Int   ]('z', "zero-crossings")
        .text(s"Kaiser window zero crossings (default = ${default.zeroCrossings}")
        .action { (x, c) => c.copy(zeroCrossings = x) }
      opt[Double]('k', "kaiser-beta")
        .text(s"Beta parameter of Kaiser windows (higher is less side lobes, wider main lobe; default: ${default.kaiserBeta})")
        .action { (x, c) => c.copy(kaiserBeta = x) }
      opt[Double]('r', "roll-off")
        .text(s"Roll-off Kaiser windows (0 to 1; default: ${default.rollOff})")
        .action { (x, c) => c.copy(rollOff = x) }
    }
    parser.parse(args, default).fold(sys.exit(1))(run)
  }

  def formatTemplate(in: File, num: Int): File = {
    val name = in.name.format(num)
    in.parentOption.fold(file(name))(_ / name)
  }

  def getImageSpec(in: File): ImageFile.Spec = {
    val iis = ImageIO.createImageInputStream(in)
    val r   = ImageIO.getImageReaders(iis).next()
    r.setInput(iis)
    val w    = r.getWidth(0)
    val h    = r.getHeight(0)
    val raw  = r.getRawImageType(0)
    val spec = if (raw != null) raw else r.getImageTypes(0).next()
    val ch   = spec.getNumComponents
    val smp  = spec.getSampleModel.getDataType match {
      case DataBuffer.TYPE_BYTE  => ImageFile.SampleFormat.Int8
      case DataBuffer.TYPE_SHORT | DataBuffer.TYPE_USHORT => ImageFile.SampleFormat.Int16
      case DataBuffer.TYPE_FLOAT => ImageFile.SampleFormat.Float
    }
    val fmt  = r.getFormatName
    val tpe  = fmt.toLowerCase(Locale.US) match {
      case "jpeg" | "jpg" => ImageFile.Type.JPG
      case "png"          => ImageFile.Type.PNG
      case _ => sys.error(s"Unsupported image format '$fmt'")
    }
    ImageFile.Spec(tpe, smp, width = w, height = h, numChannels = ch)
  }

  def run(config: Config): Unit = {
    import config._
    val firstF    = formatTemplate(templateIn, idxRange0.head)
    val imgInSpec = getImageSpec(firstF)
    import imgInSpec.{height, numChannels, width}

    val streamCfg = stream.Control.Config()
    streamCfg.blockSize  = width * 2
    streamCfg.useAsync   = false

    val g = Graph {
      import graph._
      val idxIn     = ArithmSeq(start = idxRange0.head, length = idxRange0.size) // idxRange0  .map(x => x: GE).reduce(_ ++ _)
      val idxRangeOut = 1 to math.ceil(idxRange0.size * factor).toInt
      val idxOut    = ArithmSeq(start = idxRangeOut.head, length = idxRangeOut.size) // idxRangeOut.map(x => x: GE).reduce(_ ++ _)
      val in        = ImageFileSeqIn(templateIn, numChannels = numChannels, indices = idxIn)
      val frameSize = width * height
      val resample  = ResampleWindow(in, size = frameSize, factor = factor,
        zeroCrossings = zeroCrossings, kaiserBeta = kaiserBeta, rollOff = rollOff)
      val clip      = resample.max(0.0).min(1.0)
      val sig       = clip
      val fmtOut    = templateOut.ext.toLowerCase(Locale.US) match {
        case "jpeg" | "jpg" => ImageFile.Type.JPG
        case "png" => ImageFile.Type.PNG
        case _ => imgInSpec.fileType
      }
      val smpOut    = if (fmtOut == imgInSpec.fileType) imgInSpec.sampleFormat else ImageFile.SampleFormat.Int8
      val imgOutSpec = imgInSpec.copy(fileType = fmtOut, sampleFormat = smpOut)
      ImageFileSeqOut(templateOut, imgOutSpec, indices = idxOut, in = sig)
      Progress(Frames(sig) / (frameSize.toLong * idxRangeOut.size), trig = Metro(frameSize))
    }

    lazy val gui: SimpleGUI = SimpleGUI(ctrl)
    streamCfg.progressReporter = { r =>
      Swing.onEDT {
        gui.progress = r.total
      }
    }
    lazy val ctrl = stream.Control(streamCfg)
    ctrl.run(g)

    println("Running.")
  }
}