package scala.servlet

import scala.util.IteratorEnumeration

import java.io.InputStream
import java.net.URL
import java.util.Enumeration
import java.util.EventListener
import java.util.Set
import javax.servlet._
import javax.servlet.Filter
import javax.servlet.Servlet

final class InjectableServletContext(
  initParamterMap: Map[String, String] = Map.empty,
  attributeMap: Map[String, Object] = Map.empty,
  getContext: String => ServletContext = _ => null,
  getResource: String => URL = _ => null,
) extends ServletContext {

  def getInitParameter(name: String) = initParamterMap.get(name) match {
    case Some(param) => param
    case _ => null
  }

  def getInitParameterNames = new IteratorEnumeration(initParamterMap.keys.iterator)

  def getAttribute(name: String) = attributeMap.get(name) match {
    case Some(attr) => attr
    case _ => null
  }

  def getAttributeNames = new IteratorEnumeration(attributeMap.keys.iterator)

  def getContext(uripath: String) = getContext.apply(uripath)

  def addFilter(x$1: String, x$2: Class[_ <: Filter]) = null
  def addFilter(x$1: String, x$2: Filter): FilterRegistration.Dynamic = null
  def addFilter(x$1: String, x$2: String): FilterRegistration.Dynamic = null
  def addListener(x$1: Class[_ <: EventListener]): Unit = ()
  def addListener[T <: EventListener](x$1: T): Unit = ()
  def addListener(x$1: String): Unit = ()
  def addServlet(x$1: String, x$2: Class[_ <: Servlet]): ServletRegistration.Dynamic = null
  def addServlet(x$1: String, x$2: Servlet): ServletRegistration.Dynamic = null
  def addServlet(x$1: String, x$2: String): ServletRegistration.Dynamic = null
  def createFilter[T <: Filter](x$1: Class[T]): T = throw new ServletException
  def createListener[T <: EventListener](x$1: Class[T]): T = throw new ServletException
  def createServlet[T <: Servlet](x$1: Class[T]): T = throw new ServletException
  def declareRoles(x$1: String*): Unit = ()
  def getClassLoader(): ClassLoader = null
  def getContextPath(): String = null
  def getDefaultSessionTrackingModes(): Set[SessionTrackingMode] = null
  def getEffectiveMajorVersion(): Int = 0
  def getEffectiveMinorVersion(): Int = 0
  def getEffectiveSessionTrackingModes(): Set[SessionTrackingMode] = null
  def getFilterRegistration(x$1: String): FilterRegistration = null
  def getFilterRegistrations(): java.util.Map[String, _ <: FilterRegistration] = null
  def getJspConfigDescriptor(): descriptor.JspConfigDescriptor = null
  def getMajorVersion(): Int = 0
  def getMimeType(x$1: String): String = null
  def getMinorVersion(): Int = 0
  def getNamedDispatcher(x$1: String): RequestDispatcher = null
  def getRealPath(x$1: String): String = null
  def getRequestDispatcher(x$1: String): RequestDispatcher = null
  def getResource(path: String): URL = getResource.apply(path)
  def getResourceAsStream(path: String): InputStream = getResource.apply(path).openStream()
  def getResourcePaths(x$1: String): Set[String] = null
  def getServerInfo(): String = null
  def getServlet(x$1: String): Servlet = null
  def getServletContextName(): String = null
  def getServletNames(): Enumeration[String] = null
  def getServletRegistration(x$1: String): ServletRegistration = null
  def getServletRegistrations(): java.util.Map[String, _ <: ServletRegistration] = null
  def getServlets(): Enumeration[Servlet] = null
  def getSessionCookieConfig(): SessionCookieConfig = null
  def log(x$1: String, x$2: Throwable): Unit = ()
  def log(x$1: Exception, x$2: String): Unit = ()
  def log(x$1: String): Unit = ()
  def removeAttribute(x$1: String): Unit = ()
  def setAttribute(x$1: String, x$2: Any): Unit = ()
  def setInitParameter(x$1: String, x$2: String): Boolean = false
  def setSessionTrackingModes(x$1: Set[SessionTrackingMode]): Unit = ()
}
