package com.atomist.util.lang

import com.github.javaparser.ast.`type`.{ClassOrInterfaceType, ReferenceType}
import com.github.javaparser.ast.body._
import com.github.javaparser.ast.expr._
import com.github.javaparser.ast.{CompilationUnit, ImportDeclaration, Modifier}

import scala.collection.JavaConverters._

/**
  * Utility methods to simplify working with JavaParser.
  */
object JavaParserUtils {

  import scala.language.implicitConversions

  // From https://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html
  val reservedWords = Set(
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while"
  )

  // Make calling JavaParser methods less verbose
  implicit def stringToNameExpr(s: String): NameExpr = new NameExpr(s)

//  TODO Get LexicalPreservingPrinter working
//  def getLpp(is: InputStream): Pair[ParseResult[CompilationUnit], LexicalPreservingPrinter] =
//    LexicalPreservingPrinter.setup(ParseStart.COMPILATION_UNIT, Providers.provider(is))

  // TODO why doesn't a structural type seem to work here?
  def getAnnotation[T <: BodyDeclaration[T]](bd: BodyDeclaration[T], name: String): Option[AnnotationExpr] =
    bd.getAnnotations.asScala.find(_.getNameAsString equals name)

  def getAnnotationValueAsString(an: AnnotationExpr): Option[String] = {
    an match {
      case s: SingleMemberAnnotationExpr => s.getMemberValue match {
        case sl: StringLiteralExpr => Some(sl.getValue)
      }
      // TODO fragile
      case mm: NormalAnnotationExpr => Some(mm.getPairs.get(0).getValue.asInstanceOf[StringLiteralExpr].getValue)
      case _ => None
    }
  }

  def getAnnotation(p: Parameter, name: String): Option[AnnotationExpr] =
    p.getAnnotations.asScala.find(_.getNameAsString equals name)

  import scala.language.reflectiveCalls

  // Structural type makes this work across method declarations and types
  def isPublic(modifiable: {def getModifiers: Int}) =
    java.lang.reflect.Modifier.isPublic(modifiable.getModifiers)

  def getUnderlyingType(rt: ReferenceType): ClassOrInterfaceType =
    rt.getElementType match {
      case cit: ClassOrInterfaceType => cit
      case _ => ???
    }

  def isPublicField(f: FieldDeclaration): Boolean =
    f.getModifiers.contains(Modifier.PUBLIC) && !f.getModifiers.contains(Modifier.STATIC) && f.getVariables.size == 1

  def isReservedWord(name: String): Boolean =
    name != null && name.length > 0 && reservedWords.contains(name)

  def addImportsIfNeeded(fqns: Seq[String], cu: CompilationUnit): Unit = {
    fqns.foreach(fqn => {
      val importDefinition = cu.getImports.asScala.find(_.toString contains fqn)
      if (importDefinition.isEmpty) {
        cu.getImports.add(new ImportDeclaration(new Name(fqn), false, false))
      }
    })
  }

  def removeImportsIfNeeded(fqns: Seq[String], cu: CompilationUnit): Unit = {
    fqns.foreach(fqn => {
      val importDefinition = cu.getImports.asScala.find(_.toString contains fqn)
      if (importDefinition.isDefined) {
        cu.getImports.remove(importDefinition.get)
      }
    })
  }
}
