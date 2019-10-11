package scala.servlet.http

import scala.servlet._
import scala.util.IteratorEnumeration

import javax.servlet.http._
import java.util.Enumeration
import java.util.Hashtable

final class InjectableHttpServletRequest(
  method: => String = "GET",
  protocol: => String = "HTTP/1.1",
  requestUri: => String = ???,
  headers: => Map[String, Array[String]] = Map.empty
) extends InjectableServletRequest(
  protocol = protocol,
) with HttpServletRequest {

  private lazy val lcaseHeaders = headers.map(entry => entry._1.toLowerCase() -> entry._2)

  def authenticate(x$1: javax.servlet.http.HttpServletResponse): Boolean = ???
  def getAuthType(): String = ???
  def getContextPath(): String = ???
  def getCookies(): Array[javax.servlet.http.Cookie] = ???
  def getDateHeader(x$1: String): Long = ???

  def getHeader(name: String): String = {
    lcaseHeaders.get(name.toLowerCase) match {
      case Some(headers) =>
        if (headers.length > 0)
          headers(0)
        else
          null

      case _ => null
    }
  }

  def getHeaderNames(): Enumeration[String] =
    new IteratorEnumeration(lcaseHeaders.keys.iterator)

  def getHeaders(name: String): Enumeration[String] = {
    lcaseHeaders.get(name.toLowerCase) match {
      case Some(headers) => new IteratorEnumeration(headers.iterator)
      case _ => java.util.Collections.emptyEnumeration()
    }
  }

  def getIntHeader(x$1: String): Int = ???
  def getMethod(): String = method
  def getPart(x$1: String): javax.servlet.http.Part = ???
  def getParts(): java.util.Collection[javax.servlet.http.Part] = ???
  def getPathInfo(): String = ???
  def getPathTranslated(): String = ???
  def getQueryString(): String = ???
  def getRemoteUser(): String = ???
  def getRequestURI(): String = requestUri
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
  characterEncoding: => String = "utf-8",
  setContentLength: Int => Unit = _ => ???,
  setContentType: String => Unit = _ => ???,
  outputStream: => javax.servlet.ServletOutputStream = ???,
) extends InjectableServletResponse(
  characterEncoding = characterEncoding,
  setContentLength = setContentLength,
  setContentType = setContentType,
  outputStream = outputStream,
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
