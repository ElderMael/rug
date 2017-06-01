package com.atomist.util.scalaparsing

import com.atomist.tree.content.text.LineInputPosition

import scala.util.parsing.input.OffsetPosition

/**
  * InputPosition wrapping a Scala OffsetPosition.
  */
class OffsetPositionInputPosition(of: OffsetPosition) extends LineInputPosition {

  override def offset: Int = of.offset

  def input: String = of.source.toString

  override def lineFrom1: Int = of.line

  override def colFrom1: Int = of.column

}
