package scala.ioc.servlet

import org.scalatest._

import javax.servlet.http.HttpServletResponse

package object http {

  def testPost(c: Map[Any, Any]) = {

    val resp = c(scala.ioc.servlet.IocServlet.ResponseKey).asInstanceOf[HttpServletResponse]
    import resp._

    setStatus(HttpServletResponse.SC_OK)
    setContentType("application/xhtml+xml; charset=utf-8")
    setLocale(java.util.Locale.getDefault)
    getOutputStream.println("<html><head>Test</head><body>Hello, World!</body></html>")
    flushBuffer

  }

}
