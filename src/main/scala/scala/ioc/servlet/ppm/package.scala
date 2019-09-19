package scala.ioc.servlet

import scala.collection.immutable.ListSet
import scala.ioc.ppm._
import scala.ppm._
import scala.reflect.runtime.universe
  import universe._
import scala.tools.reflect.ToolBox

import javax.servlet.ServletContext

package object ppm {
  def embedImpl(ctx: ServletContext)(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: List[Tree], tb: ToolBox[universe.type], src: Option[String]): Tree = {
    val ProcessedArgs(named, _, extraNames, leftovers) = validateThisExprAndArgs(
        expr,
        args,
        ListSet("path"),
        ListSet("encoding"),
      )

    named("path") match {
      case Literal(Constant(path: String)) => {
        val encoding =
          if (named contains "encoding")
            named("encoding") match {
              case Literal(Constant(encoding: String)) => encoding
              case _ => throw new IllegalArgumentException(
                "The optional 'encoding' argument if supplied must be a string literal")
            }
          else "utf-8"

        tb.parse(scala.io.Source.fromInputStream(ctx.getResourceAsStream(path), encoding).mkString)
      }
      case _ => throw new IllegalArgumentException(
          "'path' argument must be a string literal")
    }

  }

  def postJobResource(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: List[Tree], tb: ToolBox[universe.type], src: Option[String]): Tree = {
    val ProcessedArgs(named, _, extraNames, leftovers) = validateThisExprAndArgs(
        expr,
        args,
        ListSet("path"),
        ListSet("encoding"),
      )

    val pathExpr = named("path")
    named.get("encoding") match {
      case Some(encExpr) => q"""scala.ioc.ppm.staffFactoryFromStream(
${expr.get}.getResourceAsStream($named("path")),
$encExpr,
factory = factory,
preprocessor = preprocessor)"""
          case _ => q"""scala.ioc.ppm.staffFactoryFromStream(
${expr.get}.getResourceAsStream($named("path")),
factory = factory,
preprocessor = preprocessor)"""
    }

  }
}
