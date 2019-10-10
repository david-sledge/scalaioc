package scala.servlet.http

import scala.servlet._

import javax.servlet.http._

final class InjectableHttpServletRequest(
  method: => String = "GET",
  protocol: => String = "HTTP/1.1",
) extends InjectableServletRequest(
  protocol = protocol,
) with HttpServletRequest {
  def authenticate(x$1: javax.servlet.http.HttpServletResponse): Boolean = ???
  def getAuthType(): String = ???
  def getContextPath(): String = ???
  def getCookies(): Array[javax.servlet.http.Cookie] = ???
  def getDateHeader(x$1: String): Long = ???
  def getHeader(x$1: String): String = ???
  def getHeaderNames(): java.util.Enumeration[String] = ???
  def getHeaders(x$1: String): java.util.Enumeration[String] = ???
  def getIntHeader(x$1: String): Int = ???
  def getMethod(): String = method
  def getPart(x$1: String): javax.servlet.http.Part = ???
  def getParts(): java.util.Collection[javax.servlet.http.Part] = ???
  def getPathInfo(): String = ???
  def getPathTranslated(): String = ???
  def getQueryString(): String = ???
  def getRemoteUser(): String = ???
  def getRequestURI(): String = ???
  def getRequestURL(): StringBuffer = ???
  def getRequestedSessionId(): String = ???
  def getServletPath(): String = ???
  def getSession(): javax.servlet.http.HttpSession = ???
  def getSession(x$1: Boolean): javax.servlet.http.HttpSession = ???
  def getUserPrincipal(): java.security.Principal = ???
  def isRequestedSessionIdFromCookie(): Boolean = ???
  def isRequestedSessionIdFromURL(): Boolean = ???
  def isRequestedSessionIdFromUrl(): Boolean = ???
  def isRequestedSessionIdValid(): Boolean = ???
  def isUserInRole(x$1: String): Boolean = ???
  def login(x$1: String,x$2: String): Unit = ???
  def logout(): Unit = ???
}

final class InjectableHttpServletResponse(
  sendError: (Int, String) => Unit = (_, _) => ???,
  setHeader: (String, String) => Unit = (_, _) => ???,
  containsHeader: String => Boolean = _ => ???,
  characterEncoding: => String = ???,
  setContentLength: Int => Unit = _ => ???,
) extends InjectableServletResponse(
  characterEncoding = characterEncoding,
  setContentLength = setContentLength,
) with HttpServletResponse {
  def addCookie(x$1: javax.servlet.http.Cookie): Unit = ???
  def addDateHeader(x$1: String,x$2: Long): Unit = ???
  def addHeader(x$1: String,x$2: String): Unit = ???
  def addIntHeader(x$1: String,x$2: Int): Unit = ???
  def containsHeader(name: String): Boolean = containsHeader.apply(name)
  def encodeRedirectURL(x$1: String): String = ???
  def encodeRedirectUrl(x$1: String): String = ???
  def encodeURL(x$1: String): String = ???
  def encodeUrl(x$1: String): String = ???
  def getHeader(x$1: String): String = ???
  def getHeaderNames(): java.util.Collection[String] = ???
  def getHeaders(x$1: String): java.util.Collection[String] = ???
  def getStatus(): Int = ???
  def sendError(code: Int): Unit = sendError.apply(code, "")
  def sendError(code: Int, msg: String): Unit = sendError.apply(code, msg)
  def sendRedirect(x$1: String): Unit = ???
  def setDateHeader(x$1: String,x$2: Long): Unit = ???
  def setHeader(name: String, value: String): Unit = setHeader.apply(name, value)
  def setIntHeader(x$1: String,x$2: Int): Unit = ???
  def setStatus(x$1: Int,x$2: String): Unit = ???
  def setStatus(x$1: Int): Unit = ???
}
