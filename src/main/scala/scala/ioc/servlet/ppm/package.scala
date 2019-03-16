package scala.ioc.servlet

import scala.ioc.ppm._
import scala.collection.immutable.Seq
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

import javax.servlet.ServletContext

package object ppm {
  def embedImpl(ctx: ServletContext)(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
    // obtain toolbox
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
    val (named, _, leftovers) = mapArgs(Seq("path", "encoding"), args)
    named.get("path") match {
      case Some(Literal(Constant(path: String))) => {
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
        "'path' argument must be provided as a string literal")
    }
  }

  def postJobResource(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
    val (named, _, leftovers) = mapArgs(Seq("path", "encoding"), args);
    val missingArgs = if (named contains "path") Seq() else Seq("path")
    expr match {
      case Some(servletCtxExpr) => {
        if (missingArgs.length == 0) {
          val rsrcExpr = named.get("encoding") match {
            case Some(encExpr) => q"""scala.ioc.ppm.staffFactoryFromStream($servletCtxExpr.getResourceAsStream($named("path")),
$encExpr, factory = factory, preprocessor = preprocessor)"""
            case _ => q"""scala.ioc.ppm.staffFactoryFromStream($servletCtxExpr.getResourceAsStream($named("path")),
factory = factory, preprocessor = preprocessor)"""
          }
          Right(rsrcExpr)
        }
        else {
          Left((false, missingArgs))
        }
      }
      case _ => Left((true, missingArgs))
    }
  }
}
