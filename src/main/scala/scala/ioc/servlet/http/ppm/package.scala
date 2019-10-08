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
        "handleGet",
        "handlePost",
        "handleOptions",
        "handleDelete",
        "handlePut",
        "handleHead",
        "handleTrace",
        "handleLastModified",
      ),
    )

    /*
     * handleDelete: (HttpServletRequest, HttpServletResponse) =>
      Unit = DefaultDeleteHandler,
    handleGet: (HttpServletRequest, HttpServletResponse) =>
      Unit = DefaultGetHandler,
    optionHandleHead:
      Option[(HttpServletRequest, HttpServletResponse) => Unit] = None,
    optionHandleOptions:
      Option[(HttpServletRequest, HttpServletResponse) => Unit] = None,
    handlePost: (HttpServletRequest, HttpServletResponse) =>
      Unit = DefaultPostHandler,
    handlePut: (HttpServletRequest, HttpServletResponse) =>
      Unit = DefaultPutHandler,
    handleTrace: (HttpServletRequest, HttpServletResponse) => Unit = DefaultTraceHandler,
    getLastModified
     */
    q"""
scala.ioc.Factory.setManager(factory, "requestHandler", scala.servlet.http.createRequestHandler(${
      named.foldLeft(List[Tree]()) {
        case (acc, (name, arg)) =>
          AssignOrNamedArg(
            Ident(TermName(name)),
            q"Some(${toWorker(arg)})"
          )::acc
      }
    }))
"""
  }

}
