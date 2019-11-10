package scala.servlet.http.ioc

import scala.collection.immutable.ListSet
import scala.collection.immutable.ListMap
import scala.ioc.ppm._
import scala.ppm._
import scala.reflect.runtime.universe
  import universe._
import scala.tools.reflect.ToolBox

import javax.servlet.ServletContext

package object ppm {

  def postMethodHandlersJob(namespaceName: Option[String], localName: String)
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
      exprOpt,
      args,
      optionalArgNames = ListSet(
        "getHandler",
        "postHandler",
        "deleteHandler",
        "putHandler",
        "methodMap",
        "getLastModified",
        "id",
      ),
    )

    val id = named.getOrElse("id", q""""requestHandler"""")
    val handlers = named - "id"
    q"""
scala.ioc.Factory.setManager(
  factory,
  $id,
  scala.servlet.http.createRequestHandler2(..${
      handlers.foldLeft(List.empty[Tree]) {
        case (acc, (name, arg)) => {

          NamedArg(
            Ident(TermName(name)),
            q"Some(${scala.ioc.ppm.toWorker(arg)})",
          )::acc

        }
      }
    })(scala.servlet.http.ioc.GetHttpServletTransaction),
)
"""

  }

  def postHtmlPageResponseJob(namespaceName: Option[String], localName: String)
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
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
