package scala.ioc

import ioc._
import scala.collection.immutable.Seq
import scala.meta._
import javax.servlet.ServletContext

package object servlet {
  def includeImpl(ctx: ServletContext)(namespaceName: String, localName: String)
  (expr: Term, args: Seq[Term.Arg]): Tree = {
    val (named, _, leftovers) = mapArgs(Seq("path", "encoding"), args)
    if (named contains "path")
      named("path") match {
        case Lit.String(path) => {
          val encoding =
            if (named contains "encoding")
              named("encoding") match {
                case Lit.String(encoding) => encoding
                case _ => throw new IllegalArgumentException(
                  "The optional 'encoding' argument if supplied must be a string"
                    + " literal")
              }
            else "utf-8"
          s"{${scala.io.Source.fromInputStream(ctx.getResourceAsStream(path),
              encoding).mkString}}".parse[Term].get
        }
        case _ => throw new IllegalArgumentException(
            "'path' argument must be a string literal")
      }
    else
      throw new IllegalArgumentException(
          "'path' argument must be provided as a string literal")
  }
}
