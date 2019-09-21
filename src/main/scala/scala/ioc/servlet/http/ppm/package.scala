package scala.ioc.servlet.http

import scala.collection.immutable.ListSet
import scala.ioc.ppm._
import scala.ppm._
import scala.reflect.runtime.universe
  import universe._
import scala.tools.reflect.ToolBox

import javax.servlet.ServletContext

package object ppm {

  def postMethodHandlersJob(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: List[Tree], tb: ToolBox[universe.type], src: Option[String]): Tree = {

    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
      expr,
      args,
      optionalArgNames = ListSet(
        "options",
        "deleteHandler",
        "getHandler",
        "postHandler",
        "putHandler",
        "headHandler",
        "traceHandler",
        "getLastModified",
        "reqKey",
        "respKey",
      ),
    )

q"""
setManager(factory, "requestHandler", scala.ioc.servlet.http.RequestHandler(${
  named.foldLeft(List[Tree]())((acc, entry) =>
    AssignOrNamedArg(Ident(TermName(entry._1)), entry._2)::acc
  )
}))
"""
  }

}
