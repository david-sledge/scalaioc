package scala.servlet.http

import org.scalatest._

import javax.servlet.http._

class packageSpec extends FlatSpec with Matchers {
  "createRequestHandler()" should "return a function that accepts an HttpServletRequest object and an HttpServletResponse object" in {

    createRequestHandler() shouldBe a[(_, HttpServletResponse) => Unit]

  }
}
