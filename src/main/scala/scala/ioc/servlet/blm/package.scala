package scala.ioc.servlet

import scala.ioc.blm._
import scala.collection.immutable.Seq
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

import javax.servlet.ServletContext

package object blm {
  def includeImpl(ctx: ServletContext)(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
    // obtain toolbox
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
    val (named, _, leftovers) = mapArgs(Seq("path", "encoding"), args)
    if (named contains "path")
      named("path") match {
        case Literal(Constant(path: String)) => {
          val encoding =
            if (named contains "encoding")
              named("encoding") match {
                case Literal(Constant(encoding: String)) => encoding
                case _ => throw new IllegalArgumentException(
                  "The optional 'encoding' argument if supplied must be a string"
                    + " literal")
              }
            else "utf-8"
          Right(tb.parse(scala.io.Source.fromInputStream(ctx.getResourceAsStream(path),
              encoding).mkString))
        }
        case _ => throw new IllegalArgumentException(
            "'path' argument must be a string literal")
      }
    else
      throw new IllegalArgumentException(
          "'path' argument must be provided as a string literal")
  }
}
