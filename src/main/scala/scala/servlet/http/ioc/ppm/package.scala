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
  (expr: Option[Tree], args: List[Tree], tb: ToolBox[universe.type], src: Option[String]): Tree = {

    val argMap = ListMap(
      "handleGet" -> ("handleGet", false),
      "handlePost" -> ("handlePost", false),
      "handleDelete" -> ("handleDelete", false),
      "handlePut" -> ("handlePut", false),
      "handleTrace" -> ("handleTrace", false),
      "handleOptions" -> ("optionHandleOptions", true),
      "handleHead" -> ("optionHandleHead", true),
      "getLastModified" -> ("getLastModified", false),
    )

    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
      expr,
      args,
      optionalArgNames = argMap.foldRight(ListSet[String]()){
        case ((name, _), acc) => acc + name
      },
    )

    q"""
scala.servlet.http.createRequestHandler(..${
      named.foldLeft(List[Tree]()) {
        case (acc, (name, arg)) => {
          val (argName, isOption) = argMap(name)
          AssignOrNamedArg(
            Ident(TermName(argName)),
            if (isOption) q"Some($arg)" else q"$arg"
          )::acc
        }
      }
    })(`#$$`(scala.ioc.servlet.IocServlet.RequestKey), `#$$`(scala.ioc.servlet.IocServlet.ResponseKey))
"""
  }

}
