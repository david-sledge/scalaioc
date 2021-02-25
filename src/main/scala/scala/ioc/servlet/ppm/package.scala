package scala.ioc.servlet

import javax.servlet.ServletContext
import scala.collection.immutable.ListSet
import scala.ppm._
import scala.reflect.runtime.universe._

package object ppm {

  def embedImpl(ctx: ServletContext)(namespaceName: Option[String], localName: String)
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, _, args, tb, _) = macroArgs
    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
        exprOpt,
        args,
        ListSet("path"),
        ListSet("encoding"),
      )

    named("path") match {
      case Literal(Constant(path: String)) =>
        val encoding =
          if (named contains "encoding")
            named("encoding") match {
              case Literal(Constant(encoding: String)) => encoding
              case _ => throw new IllegalArgumentException(
                "The optional 'encoding' argument if supplied must be a string literal")
            }
          else "utf-8"

        tb.parse(scala.io.Source.fromInputStream(ctx.getResourceAsStream(path), encoding).mkString)
      case _ => throw new IllegalArgumentException(
          "'path' argument must be a string literal")
    }

  }

  def postJobResource(namespaceName: Option[String], localName: String)
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, _, args, _, _) = macroArgs
    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
        exprOpt,
        args,
        ListSet("path"),
        ListSet("encoding"),
      )

    named.get("encoding") match {
      case Some(encExpr) => q"""scala.ioc.ppm.staffFactoryFromStream(
${exprOpt.get}.getResourceAsStream(${named("path")}),
$encExpr,
factory = factory,
preprocessor = preprocessor)"""
      case _ => q"""scala.ioc.ppm.staffFactoryFromStream(
${exprOpt.get}.getResourceAsStream(${named("path")}),
factory = factory,
preprocessor = preprocessor)"""
    }

  }

}
