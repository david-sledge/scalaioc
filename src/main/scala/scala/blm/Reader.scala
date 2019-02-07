package scala.blm

import scala.reflect.runtime.universe._
import scala.io.Source._
import scala.tools.reflect.ToolBox

object Reader {
  def execute[T](code: String, preprocessor: Preprocessor): T = {
    // obtain toolbox
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
    // generate the AST
    val tree = tb.parse(code)
    // manipulate tree
    val transformedTree = preprocessor.transformMacros(tree)
    // compile
    val f = tb.compile(transformedTree)
    // ... execute
    f().asInstanceOf[T]
  }

  // TODO:  scaladoc
  def executeFromFile[T](fileName: String, encoding: String = "utf-8", preprocessor: Preprocessor) =
    execute(fromFile(fileName, encoding).mkString, preprocessor)

  // TODO:  scaladoc
  def executeFromResource[T](path: String, encoding: String = "utf-8", preprocessor: Preprocessor) =
    executeFromStream(getClass.getClassLoader.getResourceAsStream(path), encoding, preprocessor)

  // TODO:  scaladoc
  def executeFromStream[T](stream: java.io.InputStream, encoding: String = "utf-8", preprocessor: Preprocessor) =
    execute(scala.io.Source.fromInputStream(stream, encoding).mkString, preprocessor)
}