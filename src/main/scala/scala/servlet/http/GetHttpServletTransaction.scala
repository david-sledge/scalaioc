package scala.servlet.http

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

trait GetHttpServletTransaction[T] {

  def getRequest(t: T): HttpServletRequest

  def getResponse(t: T): HttpServletResponse

  def setResponse(t: T, resp: HttpServletResponse): T = t
}

object GetHttpServletTransaction {

  def getRequest[T](t: T)(implicit getRequestAndResponse: GetHttpServletTransaction[T]) = getRequestAndResponse.getRequest(t)

  def getResponse[T](t: T)(implicit getRequestAndResponse: GetHttpServletTransaction[T]) = getRequestAndResponse.getResponse(t)

  def setResponse[T](t: T, resp: HttpServletResponse)(implicit getRequestAndResponse: GetHttpServletTransaction[T]) = getRequestAndResponse.setResponse(t, resp)

}
