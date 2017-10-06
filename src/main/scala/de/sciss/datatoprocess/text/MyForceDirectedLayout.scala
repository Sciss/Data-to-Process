/*
 * MyForceDirectedLayout.scala
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

import prefuse.action.layout.graph.ForceDirectedLayout
import prefuse.visual.EdgeItem

class MyForceDirectedLayout(main: Visual)
  extends ForceDirectedLayout(Visual.GROUP_GRAPH) {

  override def getSpringLength(e: EdgeItem): Float = {
    val nSrc = e.getSourceItem
    val nDst = e.getTargetItem

    (nSrc.get(Visual.COL_MUTA), nDst.get(Visual.COL_MUTA)) match {
      case (vSrc: VisualVertex, vDst: VisualVertex) =>
        if (vSrc.lineRef eq vDst.lineRef) {
          val res = vSrc.advance
          // println(s"ADVANCE = $res")
          res
        } else -1
      case _ =>
        // println("Oh noes!")
        -1f
    }
  }

  // this is used to mark horizontal springs (using coefficient zero)
  override def getSpringCoefficient(e: EdgeItem): Float = {
    val nSrc = e.getSourceItem
    val nDst = e.getTargetItem

    (nSrc.get(Visual.COL_MUTA), nDst.get(Visual.COL_MUTA)) match {
      case (vSrc: VisualVertex, vDst: VisualVertex) =>
        if (vSrc.wordRef eq vDst.wordRef) 0f else
        if (vSrc.lineRef eq vDst.lineRef) 1f else 2f
      case _ =>
        // println("Oh noes!")
        -1f
    }
  }

  private[this] var _counter = 0

  private[this] var _stepSize = 1

  def stepSize: Int = _stepSize
  def stepSize_=(value: Int): Unit = _stepSize = value

  override def run(frac: Double): Unit = {
//    println(f"run($frac%g), counter = ${_counter}")
    var i = 0
    val n = _stepSize
    while (i < n) {
      super.run(frac)
      i += 1
    }
    _counter += n
  }

  def counter: Int = _counter
}
