package com.atomist.rug.rugdoc

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.ts.{InterfaceGenerationConfig, TypeScriptInterfaceGenerator}
import com.atomist.source.{FileArtifact, FileEditor}
import org.scalatest.{FlatSpec, Matchers}

class TypeScriptInterfaceGeneratorTest extends FlatSpec with Matchers {

  val tsc = new TypeScriptCompiler

  it should "generate compilable typescript file" in {
    val td = new TypeScriptInterfaceGenerator()
    // Make it puts the generated files where our compiler will look for them
    val output = td.generate(SimpleProjectOperationArguments("",
      Map(td.OutputPathParam -> ".atomist/editors/Interfaces.ts")))
    output.allFiles.size should be(1)

    // We need to get rid of the imports as they'll fail when we try to compile the file on its own
    val withoutImport = output ✎ new FileEditor {
      override def canAffect(f: FileArtifact): Boolean = true
      override def edit(f: FileArtifact): FileArtifact =
        f.withContent(f.content.replace(new InterfaceGenerationConfig().imports,
          """
            |interface TreeNode {}
          """.stripMargin))
    }

    val d = output.allFiles.head
    val compiled = tsc.compile(withoutImport)
    val js = compiled.allFiles.find(_.name.endsWith(".js")).get
    // println(js.content)
  }

}

/**
  * Use this to actually generate interfaces. Of course, we
  * should ultimately use the interface generator as an editor.
  */
object TypeScriptInterfaceGen extends App {

  val td = new TypeScriptInterfaceGenerator()
  // Make it puts the generated files where our compiler will look for them
  val output = td.generate(SimpleProjectOperationArguments("",
    Map(td.OutputPathParam -> ".atomist/editors/Interfaces.ts")))
  val d = output.allFiles.head

  //println(d.content)

}
