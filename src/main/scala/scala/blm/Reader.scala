package scala.blm

import scala.reflect.runtime.universe._
import scala.io.Source._
import scala.tools.reflect.ToolBox

object Reader {
  def execute[T](code: String, preprocessor: Preprocessor, src: Option[String] = None): T = {
    // obtain toolbox
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
    // generate the AST
    val tree = tb.parse(code)
    //println("Position is:  " + tree.pos)
    // manipulate tree
    val transformedTree = preprocessor.transformMacros(tree, src)
    //println(transformedTree)
    // compile
    val f = tb.compile(transformedTree)
    // ... execute
    f().asInstanceOf[T]
  }

  // TODO:  scaladoc
  def executeFromFile[T](fileName: String, encoding: String = "utf-8", preprocessor: Preprocessor) =
    execute(fromFile(fileName, encoding).mkString, preprocessor, Some(fileName))

  // TODO:  scaladoc
  def executeFromResource[T](path: String, encoding: String = "utf-8", preprocessor: Preprocessor) =
    executeFromStream(getClass.getClassLoader.getResourceAsStream(path), encoding, preprocessor, Some(path))

  // TODO:  scaladoc
  def executeFromStream[T](stream: java.io.InputStream, encoding: String = "utf-8", preprocessor: Preprocessor, src: Option[String] = None) =
    execute(scala.io.Source.fromInputStream(stream, encoding).mkString, preprocessor, src)
}
