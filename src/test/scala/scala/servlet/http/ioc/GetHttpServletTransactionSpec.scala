package scala.servlet.http.ioc

import scala.servlet.http._
import javax.servlet.http._

import org.scalatest._

class GetHttpServletTransactionSpec extends FlatSpec with Matchers {

  "createRequestHandler for IoC" should "process HTTP requests" in {

    var status = 0
    var statusMsg = ""

    val httpHandler = createRequestHandler(
      handleGet = (
        (req: HttpServletRequest, resp: HttpServletResponse) => {}
      )
    )(scala.servlet.http.ioc.GetHttpServletTransaction)

    httpHandler(Map[Any, Any](
      "req" -> new InjectableHttpServletRequest(
        method = "GET"
      ),
      "resp" -> new InjectableHttpServletResponse(
        containsHeader = name => false,
        setContentLength = _ => {},
      )
    ))

    val httpHandler2 = createRequestHandler2(
      getHandler = Some((c: Map[Any, Any]) => ())
    )(scala.servlet.http.ioc.GetHttpServletTransaction)

//    httpHandler(Map[Any, Any](
//      "req" -> new InjectableHttpServletRequest(
//        method = "GET"
//      ),
//      "resp" -> new InjectableHttpServletResponse(
//        containsHeader = name => false,
//        setContentLength = _ => {},
//      )
//    ))

  }

}
