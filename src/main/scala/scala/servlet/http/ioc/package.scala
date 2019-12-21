package scala.servlet.http

package object ioc {

  import javax.servlet.http.HttpServletRequest
  import javax.servlet.http.HttpServletResponse
  import scala.ioc.servlet.IocServlet.RequestKey
  import scala.ioc.servlet.IocServlet.ResponseKey

  implicit val IocHttpServletTransactionTypeClass =
    HttpServletTransactionTypeClass[Map[Any, Any]](
      c => c(RequestKey).asInstanceOf[HttpServletRequest],
      c => c(ResponseKey).asInstanceOf[HttpServletResponse],
      (c, resp) => c + (ResponseKey -> resp),
    )

}
