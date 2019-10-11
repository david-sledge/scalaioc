package scala.servlet

import scala.util.IteratorEnumeration
import javax.servlet._

final class InjectableServletConfig(
  servletContext: ServletContext,
  servletName: String,
  initParamterMap: Map[String, String] = Map.empty,
) extends ServletConfig {

  def getInitParameter(name: String) = initParamterMap.get(name) match {
    case Some(param) => param
    case _ => null
  }

  def getInitParameterNames = new IteratorEnumeration(initParamterMap.keys.iterator)

  def getServletContext = servletContext

  def getServletName = servletName

}
