package scala.servlet.http

import org.scalatest._

import scala.servlet.InjectableServletOutputStream
import javax.servlet.http._

class packageSpec extends FlatSpec with Matchers {

  implicit object GetTransaction extends scala.servlet.http.GetHttpServletTransaction[(HttpServletRequest, HttpServletResponse)] {

    def getRequest(c: (HttpServletRequest, HttpServletResponse)) = c._1

    def getResponse(c: (HttpServletRequest, HttpServletResponse)) = c._2

  }

  "createRequestHandler()" should "return a function" in {

    createRequestHandler()(GetTransaction) shouldBe a[_ => Unit]

  }

  it should "return a 405 status by default" in {

    var status = 0
    var statusMsg = ""
    val httpHandler = createRequestHandler()(GetTransaction)
    httpHandler.apply((
      new InjectableHttpServletRequest,
      new InjectableHttpServletResponse(
        sendError = (int, msg) => {
          status = int
          statusMsg = msg
        },
      ),
    ))

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

    httpHandler.apply((
      new InjectableHttpServletRequest(
        method = "BLAH"
      ),
      new InjectableHttpServletResponse(
        sendError = (int, msg) => {
          status = int
          statusMsg = msg
        }
      )
    ))

    status shouldBe 405
    statusMsg shouldBe java.text.MessageFormat.format(LocalStrings.getString(
        "http.method_not_implemented"), "BLAH")

  }

  it should "adjust options based on which methods have handlers" in {

    var name_ = ""
    var value_ = ""
    var httpHandler = createRequestHandler(
//      handleGet = (_, _) => {},
    )(GetTransaction)
    httpHandler.apply((
      new InjectableHttpServletRequest(
        method = "OPTIONS"
      ),
      new InjectableHttpServletResponse(
        setHeader = (name, value) => {
          name_ = name
          value_ = value
        }
      )
    ))

    name_ shouldBe "Allow"
    value_.split(", ").toSet shouldBe Set("OPTIONS", "TRACE")

    httpHandler = createRequestHandler(
      handleGet = (_, _) => {},
    )(GetTransaction)
    httpHandler.apply((
      new InjectableHttpServletRequest(
        method = "OPTIONS"
      ),
      new InjectableHttpServletResponse(
        setHeader = (name, value) => {
          name_ = name
          value_ = value
        }
      )
    ))

    name_ shouldBe "Allow"
    value_.split(", ").toSet shouldBe Set("OPTIONS", "TRACE", "GET", "HEAD")

    httpHandler = createRequestHandler(
      handlePost = (_, _) => {},
    )(GetTransaction)
    httpHandler.apply((
      new InjectableHttpServletRequest(
        method = "OPTIONS"
      ),
      new InjectableHttpServletResponse(
        setHeader = (name, value) => {
          name_ = name
          value_ = value
        }
      )
    ))

    name_ shouldBe "Allow"
    value_.split(", ").toSet shouldBe Set("POST", "OPTIONS", "TRACE")

    httpHandler = createRequestHandler(
      handleDelete = (_, _) => {},
      handleGet = (_, _) => {},
      handlePost = (_, _) => {},
      handlePut = (_, _) => {},
    )(GetTransaction)
    httpHandler.apply((
      new InjectableHttpServletRequest(
        method = "OPTIONS"
      ),
      new InjectableHttpServletResponse(
        setHeader = (name, value) => {
          name_ = name
          value_ = value
        }
      )
    ))

    name_ shouldBe "Allow"
    value_.split(", ").toSet shouldBe Set("DELETE", "GET", "HEAD", "POST", "PUT", "OPTIONS", "TRACE")

  }

  it should "not process HEAD request if it can't process GET requests" in {

    var status = 0
    var statusMsg = ""

    val httpHandler = createRequestHandler()(GetTransaction)
    httpHandler.apply((
      new InjectableHttpServletRequest(
        method = "HEAD"
      ),
      new InjectableHttpServletResponse(
        sendError = (int, msg) => {
          status = int
          statusMsg = msg
        },
        containsHeader = name => false,
        setContentLength = _ => {},
      )
    ))

    status shouldBe 405
    statusMsg shouldBe "HTTP method HEAD is not supported by this URL because GET is also not supported"

  }

  it should "process HEAD request if it can process GET requests" in {

    var status = 0
    var statusMsg = ""

    val httpHandler = createRequestHandler(
      handleGet = (req, resp) => {}
    )(GetTransaction)
    httpHandler.apply((
      new InjectableHttpServletRequest(
        method = "HEAD"
      ),
      new InjectableHttpServletResponse(
        containsHeader = name => false,
        setContentLength = _ => {},
      )
    ))

  }

  it should "always process TRACE requests" in {

    var status = 0
    var statusMsg = ""

    val httpHandler = createRequestHandler()(GetTransaction)
    httpHandler.apply((
      new InjectableHttpServletRequest(
        method = "TRACE",
        requestUri = "test",
      ),
      new InjectableHttpServletResponse(
        setContentType = _ => {},
        setContentLength = _ => {},
        outputStream = new InjectableServletOutputStream(),
      )
    ))

  }

}
