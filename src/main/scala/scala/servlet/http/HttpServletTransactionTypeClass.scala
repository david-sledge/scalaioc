package scala.servlet.http

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

final class HttpServletTransactionTypeClass[T](
  getRequest: T => HttpServletRequest,
  getResponse: T => HttpServletResponse,
  setResponse: (T, HttpServletResponse) => T,
) {

  def getRequest(t: T): HttpServletRequest = getRequest.apply(t)

  def getResponse(t: T): HttpServletResponse = getResponse.apply(t)

  def setResponse(t: T, resp: HttpServletResponse): T = setResponse.apply(t, resp)

}

object HttpServletTransactionTypeClass {
  def apply[T](
    getRequest: T => HttpServletRequest,
    getResponse: T => HttpServletResponse,
    setResponse: (T, HttpServletResponse) => T,
  ) = new HttpServletTransactionTypeClass(getRequest, getResponse, setResponse)

  def getRequest[T](t: T)(implicit transactionTypeClass: HttpServletTransactionTypeClass[T]) = transactionTypeClass.getRequest(t)

  def getResponse[T](t: T)(implicit transactionTypeClass: HttpServletTransactionTypeClass[T]) = transactionTypeClass.getResponse(t)

  def setResponse[T](t: T, resp: HttpServletResponse)(implicit transactionTypeClass: HttpServletTransactionTypeClass[T]) = transactionTypeClass.setResponse(t, resp)

}
