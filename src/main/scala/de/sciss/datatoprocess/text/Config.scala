/*
 * package.scala
 * (Miniaturen 15)
 *
 * Copyright (c) 2015-2017 Hanns Holger Rutz. All rights reserved.
 *
 * This software and music is published under the
 * Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International License
 * (CC BY-NC-ND 4.0)
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.datatoprocess.text

import de.sciss.play.json.AutoFormat
import play.api.libs.json.{JsSuccess, JsError, JsArray, JsResult, JsValue, Format}

object Config {
  implicit val format: Format[Config] = AutoFormat[Config]
}
case class Config(size: Int, lineWidth: Int,
                  speedLimit: Double, noise: Double, threshold: Int, stepSize: Int)

object Situation {
  implicit val format: Format[Situation] = AutoFormat[Situation]

  def mix(a: Situation, b: Situation, w2: Double): Situation = {
    val w1              = 1.0 - w2
    val text            = if (w2 < 1) a.text             else b.text
    val lineWidth       = if (w2 < 1) a.config.lineWidth else b.config.lineWidth
    val size            = (a.config.size      * w1 + b.config.size       * w2 + 0.5).toInt
    val speedLimit      = a.config.speedLimit * w1 + b.config.speedLimit * w2
    val noise           = a.config.noise      * w1 + b.config.noise      * w2
    val threshold       = (a.config.threshold * w1 + b.config.threshold  * w2 + 0.5).toInt
    val stepSize        = (a.config.stepSize  * w1 + b.config.stepSize   * w2 + 0.5).toInt
    val config          = Config(size = size, lineWidth = lineWidth, speedLimit = speedLimit,
      noise = noise, threshold = threshold, stepSize = stepSize)
    val forceParameters = a.forceParameters.map { case (key, map1) =>
      val map2 = b.forceParameters.getOrElse(key, map1)
      val newValues = map1.map { case (key2, v1) =>
        val v2 = map2.getOrElse(key2, v1)
        val vMix = v1 * w1 + v2 * w2
        (key2, vMix.toFloat)
      }
      (key, newValues)
    }
    Situation(config = config, forceParameters = forceParameters, text = text)
  }
}
case class Situation(config: Config, forceParameters: Map[String, Map[String, Float]], text: String) {
  override def toString = s"[$config, $forceParameters, $text]"
}

object KeyFrame {
  implicit val format: Format[KeyFrame] = AutoFormat[KeyFrame]
}
case class KeyFrame(frame: Int, situation: Situation) {
  override def toString = s"$frame: $situation"
}

// type Anim = Vec[(Int, Map[String, Map[String, Float]])]
object Anim {
  def empty = Vector.empty[KeyFrame]

  implicit val format: Format[Anim] = new Format[Anim] {
    def reads(json: JsValue): JsResult[Anim] = json match {
      case JsArray(seq) =>
        ((JsSuccess(Anim.empty): JsResult[Anim]) /: seq) { (res, js) =>
          res.flatMap(in => KeyFrame.format.reads(js).map(in :+ _))
        }

      case _ => JsError("Not an array")
    }

    def writes(anim: Anim): JsValue = {
      val inner = anim.map(KeyFrame.format.writes)
      JsArray(inner)
    }
  }
}