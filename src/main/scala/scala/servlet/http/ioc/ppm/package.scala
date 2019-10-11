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

    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
      expr,
      args,
      optionalArgNames = ListSet(
        "handleGet",
        "handlePost",
        "handleDelete",
        "handlePut",
        "methodMap",
        "getLastModified",
      ),
    )

    q"""
scala.servlet.http.createRequestHandler(..${
      named.foldLeft(List.empty[Tree]) {
        case (acc, (name, arg)) => {

          AssignOrNamedArg(
            Ident(TermName(name)),
            arg,
          )::acc

        }
      }
    })(`#$$`(scala.ioc.servlet.IocServlet.RequestKey), `#$$`(scala.ioc.servlet.IocServlet.ResponseKey))
"""
  }

}
