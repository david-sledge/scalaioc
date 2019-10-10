package scala.servlet.http

import org.scalatest._

import javax.servlet.http._

class packageSpec extends FlatSpec with Matchers {

  "createRequestHandler()" should "return a function that accepts an HttpServletRequest object and an HttpServletResponse object" in {

    createRequestHandler() shouldBe a[(_, HttpServletResponse) => Unit]

  }

  it should "return a 405 status by default" in {

    var status = 0
    var statusMsg = ""
    val httpHandler = createRequestHandler()
    httpHandler.apply(
      new InjectableHttpServletRequest,
      new InjectableHttpServletResponse(
        sendError = (int, msg) => {
          status = int
          statusMsg = msg
        }
      )
    )

    status shouldBe 405
    statusMsg shouldBe LocalStrings.getString("http.method_get_not_supported")

    httpHandler.apply(
      new InjectableHttpServletRequest(
        method = "POST"
      ),
      new InjectableHttpServletResponse(
        sendError = (int, msg) => {
          status = int
          statusMsg = msg
        }
      )
    )

    status shouldBe 405
    statusMsg shouldBe LocalStrings.getString("http.method_post_not_supported")

    httpHandler.apply(
      new InjectableHttpServletRequest(
        method = "BLAH"
      ),
      new InjectableHttpServletResponse(
        sendError = (int, msg) => {
          status = int
          statusMsg = msg
        }
      )
    )

    status shouldBe 405
    statusMsg shouldBe java.text.MessageFormat.format(LocalStrings.getString(
        "http.method_not_implemented"), "BLAH")

  }

}
