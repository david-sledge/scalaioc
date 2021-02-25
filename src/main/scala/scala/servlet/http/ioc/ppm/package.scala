package scala.servlet.http.ioc

import scala.collection.immutable.ListSet
import scala.ppm._
import scala.reflect.runtime.universe._

package object ppm {

  def postMethodHandlersJob(namespaceName: Option[String], localName: String)
  (macroArgs: MacroArgs): Tree = {

    val methodMap = "methodMap"
    val id = "id"
    val getLastModified = "getLastModified"
    val optionalArgNames = ListSet(
      methodMap,
      id,
      getLastModified,
    )
    val MacroArgs(exprOpt, _, args, _, _) = macroArgs
    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
      exprOpt,
      args,
      ListSet(),
      optionalArgNames,
    )

    val args1 = named.get(getLastModified) match {
      case Some(tree) => List(NamedArg(
          Ident(TermName(getLastModified)),
          q"Some(${scala.ioc.ppm.toWorker(tree)})",
        ))
      case _ => Nil
    }

    q"""
scala.ioc.Factory.setManager(
  factory,
  ${named.getOrElse(id, q""""requestHandler"""")},
  scala.servlet.http.createRequestHandler(..${
    named.get(methodMap) match {
      case Some(tree) => NamedArg(
          Ident(TermName(methodMap)),
          tree,
        )::args1
      case _ => args1
    }
  })(scala.servlet.http.ioc.IocHttpServletTransactionTypeClass),
)
"""

  }

  def postMethodMap(namespaceName: Option[String], localName: String)
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
    val ProcessedArgs(named, _, _, leftovers) = validateThisExprAndArgs(
      exprOpt,
      args,
      allowExtraNamedArgs = true,
      allowExcessOrdinalArgs = true,
    )

    q"""
Map[String, Map[Any, Any] => Unit](
  ..${
      named.foldLeft(
        leftovers.getOrElse(throw new Exception("Programmatic error. Flog the developer!"))
      )(
        (acc, entry) =>
          q"${Literal(Constant(entry._1))} -> ${scala.ioc.ppm.toWorker(entry._2)}"::acc
      )
    }
)
"""

  }

  def postHtmlPageResponseJob(namespaceName: Option[String], localName: String)
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, _, args, _, _) = macroArgs
    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
      exprOpt,
      args,
      ListSet("page"),
      ListSet(
        "status",
        "enc",
      ),
    )

    q"""
{
  val resp: javax.servlet.http.HttpServletResponse = `#scalaioc#$$`("resp")

  `#scalaioc#let`(
    "enc" -> ${named.getOrElse("enc", q""""utf-8"""")},
    "xmlWriter" ->
      javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(resp.getOutputStream, `#scalaioc#$$`("enc")),
    {
      resp.setStatus(${named.getOrElse("status", q"javax.servlet.http.HttpServletResponse.SC_OK")})
      resp.setContentType(s"application/xhtml+xml; charset=$${`#scalaioc#$$`("enc")}")
      resp.setLocale(java.util.Locale.getDefault)
      `#scalaioc.servlet#embed`(${named("page")})
      resp.flushBuffer
    }
  )
}
"""
  }

}
