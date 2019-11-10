package scala.servlet.http

package object ioc {

  import javax.servlet.http.HttpServletRequest
  import javax.servlet.http.HttpServletResponse
  import scala.ioc.servlet.IocServlet.RequestKey
  import scala.ioc.servlet.IocServlet.ResponseKey

  implicit object GetHttpServletTransaction extends scala.servlet.http.GetHttpServletTransaction[Map[Any, Any]] {

    def getRequest(c: Map[Any, Any]) = {
      c(RequestKey).asInstanceOf[HttpServletRequest]
    }

    def getResponse(c: Map[Any, Any]) = {
      c(ResponseKey).asInstanceOf[HttpServletResponse]
    }

  }

}