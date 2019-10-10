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
        },
      ),
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

  it should "adjust options based on which methods have handlers" in {

    var name_ = ""
    var value_ = ""
    var httpHandler = createRequestHandler(
//      handleGet = (_, _) => {},
    )
    httpHandler.apply(
      new InjectableHttpServletRequest(
        method = "OPTIONS"
      ),
      new InjectableHttpServletResponse(
        setHeader = (name, value) => {
          name_ = name
          value_ = value
        }
      )
    )

    name_ shouldBe "Allow"
    value_ shouldBe "OPTIONS, TRACE"

    httpHandler = createRequestHandler(
      handleGet = (_, _) => {},
    )
    httpHandler.apply(
      new InjectableHttpServletRequest(
        method = "OPTIONS"
      ),
      new InjectableHttpServletResponse(
        setHeader = (name, value) => {
          name_ = name
          value_ = value
        }
      )
    )

    name_ shouldBe "Allow"
    value_ shouldBe "GET, HEAD, OPTIONS, TRACE"

    httpHandler = createRequestHandler(
      handlePost = (_, _) => {},
    )
    httpHandler.apply(
      new InjectableHttpServletRequest(
        method = "OPTIONS"
      ),
      new InjectableHttpServletResponse(
        setHeader = (name, value) => {
          name_ = name
          value_ = value
        }
      )
    )

    name_ shouldBe "Allow"
    value_ shouldBe "POST, OPTIONS, TRACE"

    httpHandler = createRequestHandler(
      handleDelete = (_, _) => {},
      handleGet = (_, _) => {},
      handlePost = (_, _) => {},
      handlePut = (_, _) => {},
    )
    httpHandler.apply(
      new InjectableHttpServletRequest(
        method = "OPTIONS"
      ),
      new InjectableHttpServletResponse(
        setHeader = (name, value) => {
          name_ = name
          value_ = value
        }
      )
    )

    name_ shouldBe "Allow"
    value_ shouldBe "DELETE, GET, HEAD, POST, PUT, OPTIONS, TRACE"

  }

  it should "process HEAD request if it can process GET requests" in {

    var status = 0
    var statusMsg = ""

    val httpHandler = createRequestHandler()
    httpHandler.apply(
      new InjectableHttpServletRequest(
        method = "HEAD"
      ),
      new InjectableHttpServletResponse(
        sendError = (int, msg) => {
          status = int
          statusMsg = msg
        },
        containsHeader = name => false,
        characterEncoding = "utf-8",
        setContentLength = _ => {},
      )
    )

    status shouldBe 405
    statusMsg shouldBe "HTTP method HEAD is not supported by this URL because GET is also not supported"

  }

}
