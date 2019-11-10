package scala.servlet

import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import scala.jdk.CollectionConverters._

class InjectableServletRequest(
  protocol: => String = ???,
  paramterMap: Map[String, Array[String]] = Map.empty,
) extends ServletRequest {
  def getAsyncContext(): javax.servlet.AsyncContext = ???
  def getAttribute(x$1: String): Object = ???
  def getAttributeNames(): java.util.Enumeration[String] = ???
  def getCharacterEncoding(): String = ???
  def getContentLength(): Int = ???
  def getContentType(): String = ???
  def getDispatcherType(): javax.servlet.DispatcherType = ???
  def getInputStream(): javax.servlet.ServletInputStream = ???
  def getLocalAddr(): String = ???
  def getLocalName(): String = ???
  def getLocalPort(): Int = ???
  def getLocale(): java.util.Locale = ???
  def getLocales(): java.util.Enumeration[java.util.Locale] = ???
  def getParameter(name: String): String = paramterMap.get(name) match {
    case Some(values) => {
      if (values.size > 0) {
        values(0)
      }
      else {
        null
      }
    }
    case _ => null
  }
  def getParameterMap(): java.util.Map[String,Array[String]] = paramterMap.asJava
  def getParameterNames(): java.util.Enumeration[String] = new scala.util.IteratorEnumeration(paramterMap.keys.iterator)
  def getParameterValues(name: String): Array[String] = paramterMap.get(name) match {
    case Some(values) => values
    case _ => null
  }
  def getProtocol(): String = protocol
  def getReader(): java.io.BufferedReader = ???
  def getRealPath(x$1: String): String = ???
  def getRemoteAddr(): String = ???
  def getRemoteHost(): String = ???
  def getRemotePort(): Int = ???
  def getRequestDispatcher(x$1: String): javax.servlet.RequestDispatcher = ???
  def getScheme(): String = ???
  def getServerName(): String = ???
  def getServerPort(): Int = ???
  def getServletContext(): javax.servlet.ServletContext = ???
  def isAsyncStarted(): Boolean = ???
  def isAsyncSupported(): Boolean = ???
  def isSecure(): Boolean = ???
  def removeAttribute(x$1: String): Unit = ???
  def setAttribute(x$1: String,x$2: Any): Unit = ???
  def setCharacterEncoding(x$1: String): Unit = ???
  def startAsync(x$1: javax.servlet.ServletRequest,x$2: javax.servlet.ServletResponse): javax.servlet.AsyncContext = ???
  def startAsync(): javax.servlet.AsyncContext = ???
}

class InjectableServletResponse(
  characterEncoding: => String = ???,
  setContentLength: Int => Unit = _ => ???,
  setContentType: String => Unit = _ => ???,
  outputStream: => javax.servlet.ServletOutputStream = ???,
  setLocale: java.util.Locale => Unit = local => ???,
  flushBuffer: () => Unit = () => ???,
) extends ServletResponse {
  def flushBuffer(): Unit = flushBuffer.apply()
  def getBufferSize(): Int = ???
  def getCharacterEncoding(): String = characterEncoding
  def getContentType(): String = ???
  def getLocale(): java.util.Locale = ???
  def getOutputStream(): javax.servlet.ServletOutputStream = outputStream
  def getWriter(): java.io.PrintWriter = ???
  def isCommitted(): Boolean = ???
  def reset(): Unit = ???
  def resetBuffer(): Unit = ???
  def setBufferSize(x$1: Int): Unit = ???
  def setCharacterEncoding(x$1: String): Unit = ???
  def setContentLength(len: Int): Unit = setContentLength.apply(len)
  def setContentType(typ: String): Unit = setContentType.apply(typ)
  def setLocale(locale: java.util.Locale): Unit = setLocale.apply(locale)
}
