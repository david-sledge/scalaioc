package scala.servlet.http.ioc

import scala.servlet.http._
import javax.servlet.http._

import org.scalatest._

class HttpServletTransactionTypeClassSpec extends FlatSpec with Matchers {

  "createRequestHandler for IoC" should "process HTTP requests" in {

    var status = 0
    var statusMsg = ""

    val httpHandler = createRequestHandler(Map(
      "GET" -> (
        (_) => {}
      )),
    )(IocHttpServletTransactionTypeClass)

    httpHandler(Map[Any, Any](
      "req" -> new InjectableHttpServletRequest(
        method = "GET"
      ),
      "resp" -> new InjectableHttpServletResponse(
        containsHeader = name => false,
        setContentLength = _ => {},
      )
    ))

  }

}
