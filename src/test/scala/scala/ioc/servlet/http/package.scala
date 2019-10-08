package scala.ioc.servlet

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

package object http {

  def testPost(req: HttpServletRequest, resp: HttpServletResponse) = {

    import resp._

    setStatus(HttpServletResponse.SC_OK)
    setContentType("application/xhtml+xml; charset=utf-8")
    setLocale(java.util.Locale.getDefault)
    getOutputStream.println("<html><head>Test</head><body>Hello, World!</body></html>")
    flushBuffer

  }

}
