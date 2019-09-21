package scala.ioc.servlet

import java.util.Dictionary
import java.util.Hashtable
import javax.servlet._

final class InjectableServletConfig(
  servletContext: ServletContext,
  servletName: String,
  initParamterDict: Dictionary[String, String] = new Hashtable(),
) extends ServletConfig {

  def getInitParameter(name: String) = initParamterDict.get(name)

  def getInitParameterNames = initParamterDict.keys

  def getServletContext = servletContext

  def getServletName = servletName

}
