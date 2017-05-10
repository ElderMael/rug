package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.TreeNode
import com.atomist.tree.TreeNode.Significance
import com.atomist.tree.content.text.microgrammar.Matcher.MatchPrefixResult
import com.atomist.tree.content.text.{InputPosition, MutableTerminalTreeNode, OffsetInputPosition, SimpleMutableContainerTreeNode}

/**
  * Match 0 or more occurrences of a node.
  * We create a new subnode with the given name
  *
  * @param m         matcher that may match 0 or more times
  * @param separator separator. If this is supplied, this is handled as a repsep rather than a straight rep
  */
case class Rep(m: Matcher, givenName: Option[String] = None, separator: Option[Matcher] = None)
  extends Matcher {

  def name: String = givenName.getOrElse(".rep")

  override def shortDescription(knownMatchers: Map[String, Matcher]): String = s"Rep(${m.shortDescription(knownMatchers)})"

  private val secondaryMatch: Matcher = separator match {
    case None => m
    case Some(sep) => Discard(sep) ~? m
  }

  private val treeNodeSignificance = if (givenName.isDefined) TreeNode.Signal else TreeNode.Noise

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult =
    m.matchPrefix(inputState) match {
      case Left(_) =>
        // We can match zero times. Put in an empty node.
        val pos = inputState.inputPosition
        Right(
          PatternMatch(node = EmptyContainerTreeNode(name, pos),
            matched = "", inputState, this.toString))
      case Right(initialMatch) =>
        // We matched once. Let's keep going
        //println(s"Found initial match for $initialMatch")
        var matched = initialMatch.matched
        var latestInputState = initialMatch.resultingInputState
        var nodes = Seq(initialMatch.node)
        //println(s"Trying secondary match $secondaryMatch against [${s.toString.substring(upToOffset)}]")
        while (secondaryMatch.matchPrefix(latestInputState) match {
          case Left(_) => false
          case Right(lastMatch) =>
            //println(s"Made it to secondary match [$lastMatch]")
            matched += lastMatch.matched
            nodes ++= Seq(lastMatch.node)
            latestInputState = lastMatch.resultingInputState
            true
        }) {
          // Do nothing. The nasty vars are already being updated. Nasty vars
        }

        val pos = inputState.inputPosition
        val endpos = if (nodes.isEmpty) pos else nodes.last.endPosition
        val combinedNode = new SimpleMutableContainerTreeNode(name, nodes, pos, endpos, treeNodeSignificance)
        Right(
          PatternMatch(node = combinedNode,
            matched, latestInputState, this.toString)
        )
    }
}

object Repsep {

  def apply(m: Matcher, sep: Matcher, name: Option[String]): Matcher =
    Rep(m, name, Some(sep))
}

private case class EmptyContainerTreeNode(name: String, pos: InputPosition, override val significance: Significance = TreeNode.Undeclared)
  extends MutableTerminalTreeNode(name, "", pos, significance) {
  // TODO: why does container extend terminal this is bad

  override def endPosition: InputPosition = startPosition

  addType("empty")

  override def value: String = ""
}