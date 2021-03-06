package com.atomist.rug.kind.impact

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.{SimpleContainerGraphNode, SimpleExecutionContext}
import com.atomist.rug.test.gherkin.handler.MutableRepoResolver
import com.atomist.source._
import com.atomist.tree.SimpleTerminalTreeNode
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult
import com.atomist.tree.pathexpression.{PathExpression, PathExpressionEngine}
import com.atomist.tree.utils.NodeUtils
import org.scalatest.{FlatSpec, Matchers}

class ImpactTypeTest extends FlatSpec with Matchers {

  import com.atomist.tree.pathexpression.PathExpressionParser._

  val pe = new PathExpressionEngine

  "Impact type" should "resolve Impact under Push" in {
    val oldAs = SimpleFileBasedArtifactSource()
    val newAs = oldAs + StringFileArtifact("README.md", "Add stuff to this project")
    evalInPush(oldAs, newAs, "/Push()[/Repo()]/with::Impact()") match {
      case Right(l) =>
        assert(l.nonEmpty)
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "resolve new file under Push" in
    resolveNewFile("/Push()[/Repo()]/with::Impact()/files")

  it should "resolve new file under Push matching name and type" in
    resolveNewFile("/Push()[/Repo()]/with::Impact()/files::FileAddition()")

  it should "resolve new file under Push matching name and type with predicate" in
    resolveNewFile("/Push()[/Repo()]/with::Impact()/files::FileAddition()[/file[@path='README.md']]")

  it should "resolve file update under Push" in
    resolveUpdatedFile("/Push()[/Repo()]/with::Impact()/files::FileUpdate()")

  it should "resolve file update under Push matching name and type with predicate" in
    resolveUpdatedFile("/Push()[/Repo()]/with::Impact()/files::FileUpdate()[@path='README.md']")

  it should "resolve file update under Push matching name and type with predicates" in
    resolveUpdatedFile("/Push()[/Repo()]/with::Impact()/files::FileUpdate()[@path='README.md']" +
      "[/old::File()[@path='README.md']][/new::File()[@path='README.md']]")

  it should "resolve file deletion under Push" in
    resolveDeletedFile("/Push()[/Repo()]/with::Impact()/files::FileDeletion()")

  it should "drill into changed as project" in {
    val oldAs = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "<xml></xml>"))
    val newAs = oldAs + StringFileArtifact("README.md", "Add stuff to this project")
    evalInPush(oldAs, newAs, "/Push()[/Repo()]/with::Impact()/changed::Project()") match {
      case Right(l) =>
        assert(l.size === 1)
        val changedAsProject = l.head.asInstanceOf[ProjectMutableView]
        assert(changedAsProject.totalFileCount === 1)
        changedAsProject.findFile("README.md") should not be null
      case x => fail(s"Unexpected: $x")
    }
  }

  it should "resolve under PullRequest" in {
    val oldAs = SimpleFileBasedArtifactSource(StringFileArtifact("pom.xml", "<xml></xml>"))
    val newAs = oldAs + StringFileArtifact("README.md", "Add stuff to this project")
    evalInPullRequest(oldAs, newAs, "/PullRequest()[/Repo()]/with::Impact()/changed::Project()") match {
      case Right(l) =>
        assert(l.size === 1)
        val changedAsProject = l.head.asInstanceOf[ProjectMutableView]
        assert(changedAsProject.totalFileCount === 1)
        changedAsProject.findFile("README.md") should not be null
      case x => fail(s"Unexpected: $x")
    }
  }

  private def resolveNewFile(expr: String): Unit = {
    val oldAs = SimpleFileBasedArtifactSource()
    val newAs = oldAs + StringFileArtifact("README.md", "Add stuff to this project")
    evalInPush(oldAs, newAs, expr) match {
      case Right(l) =>
        assert(l.size === 1)
        val newFile = l.head
        assert(newFile.hasTag("FileAddition"))
        assert(NodeUtils.requiredKeyValue(newFile, "path") === "README.md")
      case x => fail(s"Unexpected: $x")
    }
  }

  private def resolveDeletedFile(expr: String): Unit = {
    val oldAs = EmptyArtifactSource() + StringFileArtifact("README.md", "Add stuff to this project")
    val newAs = oldAs - "README.md"

    evalInPush(oldAs, newAs, expr) match {
      case Right(l) =>
        assert(l.size === 1)
        val newFile = l.head
        assert(newFile.hasTag("FileDeletion"))
        assert(NodeUtils.requiredKeyValue(newFile, "path") === "README.md")
      case x => fail(s"Unexpected: $x")
    }
  }

  private def resolveUpdatedFile(expr: String): Unit = {
    val oldAs = SimpleFileBasedArtifactSource(StringFileArtifact("README.md", "OLD: Add stuff to this project"))
    val newAs = oldAs.edit(new FileEditor {
      override def canAffect(f: FileArtifact) = true
      override def edit(f: FileArtifact): FileArtifact = f.path match {
        case "README.md" => f.withContent("NEW: Add stuff to this project")
        case _ => f
      }
    })
    evalInPush(oldAs, newAs, expr) match {
      case Right(l) =>
        assert(l.size === 1)
        val newFile = l.head
        assert(newFile.hasTag("FileUpdate"))
        assert(NodeUtils.requiredKeyValue(newFile, "path") === "README.md")
      case x => fail(s"Unexpected: $x")
    }
  }

  private def evalInPush(oldAs: ArtifactSource, newAs: ArtifactSource, pex: PathExpression): ExecutionResult = {
    val (owner, repo) = ("atomist", "rug")
    val afterSha = "xxxx"
    val beforeSha = "yyyy"
    val rr = new MutableRepoResolver
    rr.defineRepo(owner, repo, beforeSha, oldAs)
    rr.defineRepo(owner, repo, afterSha, newAs)

    val ec = SimpleExecutionContext(DefaultTypeRegistry,
      Some(rr))
    val repoNode = SimpleContainerGraphNode.empty("Repo", "Repo")
      .addRelatedNode(SimpleTerminalTreeNode("owner", owner))
      .addRelatedNode(SimpleTerminalTreeNode("name", repo))
    val beforeCommit = SimpleContainerGraphNode.empty("Commit", "Commit")
      .addRelatedNode(SimpleTerminalTreeNode("sha", beforeSha))
    val afterCommit = SimpleContainerGraphNode.empty("Commit", "Commit")
      .addRelatedNode(SimpleTerminalTreeNode("sha", afterSha))
      .addRelatedNode(repoNode)
    val pushNode = SimpleContainerGraphNode.empty("Push", "Push")
      .addRelatedNode(repoNode)
      .addEdge("before", Seq(beforeCommit))
      .addEdge("after", Seq(afterCommit))
    pe.evaluate(SimpleContainerGraphNode("root", pushNode), pex, ec)
  }

  private def evalInPullRequest(oldAs: ArtifactSource, newAs: ArtifactSource, pex: PathExpression): ExecutionResult = {
    val (owner, repo) = ("atomist", "rug")
    val afterSha = "xxxx"
    val beforeSha = "yyyy"
    val rr = new MutableRepoResolver
    rr.defineRepo(owner, repo, beforeSha, oldAs)
    rr.defineRepo(owner, repo, afterSha, newAs)

    val ec = SimpleExecutionContext(DefaultTypeRegistry,
      Some(rr))
    val repoNode = SimpleContainerGraphNode.empty("Repo", "Repo")
      .addRelatedNode(SimpleTerminalTreeNode("owner", owner))
      .addRelatedNode(SimpleTerminalTreeNode("name", repo))
    val beforeCommit = SimpleContainerGraphNode.empty("Commit", "Commit")
      .addRelatedNode(SimpleTerminalTreeNode("sha", beforeSha))
    val afterCommit = SimpleContainerGraphNode.empty("Commit", "Commit")
      .addRelatedNode(SimpleTerminalTreeNode("sha", afterSha))
      .addRelatedNode(repoNode)
    val prNode = SimpleContainerGraphNode.empty("PullRequest", "PullRequest")
      .addRelatedNode(repoNode)
      .addEdge("base", Seq(beforeCommit))
      .addEdge("head", Seq(afterCommit))
    pe.evaluate(SimpleContainerGraphNode("root", prNode), pex, ec)
  }

}
