package com.atomist.rug.kind.docker

class DockerfileLine(val name: String) {

  private var lineno: Int = 0
  private var raw: String = _
  private var args: Any = _

  def getLineno = lineno

  def setLineno(lineno: Int) =
    this.lineno = lineno

  def getArgs: Any = args match {
    case s: String => s.replace("#$#", "")
    case _ => args
  }

  def setArgs(args: Any) = this.args = args match {
    case s: String => s.replace("#$#", "")
    case _ => args
  }

  def getRaw: String =
    if ("COMMENT".equals(name))
    args.toString
   else
    raw

  def setRaw(raw: String) =
    this.raw = raw.replace("#$#", "\\\n")

  override def toString: String = raw
}
