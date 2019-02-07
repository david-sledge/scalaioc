package scala.servlet.http

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls

import java.io.IOException
import java.io.PrintWriter
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import java.lang.reflect.Method
import java.text.MessageFormat
import java.util.Enumeration
import java.util.Locale
import java.util.ResourceBundle

import javax.servlet.ServletException
import javax.servlet.ServletOutputStream
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

object HttpRequestHandler {
  val MethodDelete = "DELETE"
  val MethodGet = "GET"
  val MethodHead = "HEAD"
  val MethodOptions = "OPTIONS"
  val MethodPost = "POST"
  val MethodPut = "PUT"
  val MethodTrace = "TRACE"

  val HeaderIfModSince = "If-Modified-Since"
  val HeaderLastMod = "Last-Modified"

  val LocalStringsFile = "javax.servlet.http.LocalStrings"
  val lStrings = ResourceBundle.getBundle(LocalStringsFile)

  def defaultHandler(lStringName: String):
  (HttpServletRequest, HttpServletResponse) => Unit =
    (req: HttpServletRequest, resp: HttpServletResponse) => {
    	val msg = lStrings.getString(lStringName);

    	if (req.getProtocol.endsWith("1.1"))
    	    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
    	else
    	    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }

  def defaultDeleteHandler(req: HttpServletRequest, resp: HttpServletResponse): Unit =
    defaultHandler("http.method_delete_not_supported")

  def defaultGetHandler(req: HttpServletRequest, resp: HttpServletResponse): Unit =
    defaultHandler("http.method_get_not_supported")

  def defaultPostHandler(req: HttpServletRequest, resp: HttpServletResponse): Unit =
    defaultHandler("http.method_post_not_supported")

  def defaultPutHandler(req: HttpServletRequest, resp: HttpServletResponse): Unit =
    defaultHandler("http.method_put_not_supported")

  def apply(
    handleDelete: (HttpServletRequest, HttpServletResponse) =>
      Unit = HttpRequestHandler.defaultDeleteHandler,
    handleGet: (HttpServletRequest, HttpServletResponse) =>
      Unit = HttpRequestHandler.defaultGetHandler,
    handlePost: (HttpServletRequest, HttpServletResponse) =>
      Unit = HttpRequestHandler.defaultPostHandler,
    handlePut: (HttpServletRequest, HttpServletResponse) =>
      Unit = HttpRequestHandler.defaultPutHandler,
    optionHandleHead:
      Option[(HttpServletRequest, HttpServletResponse) => Unit] = None,
    optionHandleOptions:
      Option[(HttpServletRequest, HttpServletResponse) => Unit] = None,
    optionHandleTrace:
      Option[(HttpServletRequest, HttpServletResponse) => Unit] = None,
    getLastModified: (HttpServletRequest) => Long = (req) => -1,
    optionHandleRequest:
      Option[(HttpServletRequest, HttpServletResponse) => Unit] = None
  ) = {

    optionHandleRequest match {
      case Some(handleRequest) => handleRequest
      case _ => {

        val handleHead =
          optionHandleHead match {
            case Some(handleHead) => handleHead
            case _ =>
            (req: HttpServletRequest, resp: HttpServletResponse) => {

              val response = new HttpServletResponseWrapper(resp) {

                val noBody = new ServletOutputStream {

            	    private var contentLength = 0

            	    def getContentLength: Int = contentLength

            	    def write(b: Int) =
            	      contentLength += 1

          	      override def write(buf: Array[Byte], offset: Int, len: Int) =
          	      {
            	      if (len >= 0)
            	        contentLength += len
            	      else
            	        throw new IllegalArgumentException(
            	            HttpRequestHandler.lStrings.getString(
            	                "err.io.negativelength"))
          	      }
          	    }

                val writer = new PrintWriter(
                    new OutputStreamWriter(noBody, getCharacterEncoding))
                var didSetContentLength = false

                def setContentLength: Unit =
                  if (!didSetContentLength) {
                    writer.flush

                    setContentLength(noBody.getContentLength)
                  }

                override def setContentLength(len: Int) = {
                  super.setContentLength(len)
                  didSetContentLength = true
                }

                override def getOutputStream = noBody

                override def getWriter = writer
              }

            	handleGet(req, response)
              response.setContentLength
            }
        }

        val handleOptions =
          optionHandleOptions match {
            case Some(handleOptions) => handleOptions
            case _ =>
              (req: HttpServletRequest, resp: HttpServletResponse) => {
                def template(handler: (HttpServletRequest, HttpServletResponse) => Unit,
                    defaultHandler: (HttpServletRequest, HttpServletResponse) => Unit,
                    method: String) =
                  (allow: List[String]) =>
                    if (handler == defaultHandler) allow else method :: allow

                resp.setHeader("Allow", template(handleDelete, HttpRequestHandler.defaultDeleteHandler,
                    HttpRequestHandler.MethodDelete)(
                ((allow: List[String]) =>
                  if (handleGet == (HttpRequestHandler.defaultGetHandler(_, _)))
                    allow
                  else
                    HttpRequestHandler.MethodGet :: HttpRequestHandler.MethodHead :: allow)(
                template(handlePost, HttpRequestHandler.defaultPostHandler,
                    HttpRequestHandler.MethodPost)(
                template(handlePut, HttpRequestHandler.defaultPutHandler,
                    HttpRequestHandler.MethodPut)(
                        List[String](
                            HttpRequestHandler.MethodOptions,
                            HttpRequestHandler.MethodTrace))))).mkString(", "))
              }
          }

        val handleTrace =
          optionHandleTrace match {
            case Some(handleTrace) => handleTrace
            case _ =>
              (req: HttpServletRequest, resp: HttpServletResponse) => {

                  val CRLF = "\r\n"
                  val response = 
                      ("TRACE " :: req.getRequestURI :: " " :: req.getProtocol ::
                        (req.getHeaderNames.asScala.toList /: List(CRLF))((acc, s) => {
                            CRLF :: s :: ": " :: req.getHeader(s) :: acc
                          })).mkString
                  resp.setContentType("message/http")
                  resp.setContentLength(response.length)
                  resp.getOutputStream.print(response)
              }
          }

        def maybeSetLastModified(resp: HttpServletResponse, lastModified: Long) = {
          if (!resp.containsHeader(HttpRequestHandler.HeaderLastMod) && lastModified >= 0)
            resp.setDateHeader(HttpRequestHandler.HeaderLastMod, lastModified);
        }

        (req: HttpServletRequest, resp: HttpServletResponse) => {

          val methodMap =
            Map[String, (HttpServletRequest, HttpServletResponse) => Unit](
              HttpRequestHandler.MethodGet -> ((req, resp) => {
                val lastModified = getLastModified(req)

                if (lastModified == -1)
                  // servlet doesn't support if-modified-since, no reason
                  // to go through further expensive logic
                  handleGet(req, resp)
                else {
                  val ifModifiedSince = req.getDateHeader(HttpRequestHandler.HeaderIfModSince);

                  if (ifModifiedSince < lastModified) {
              	    // If the servlet mod time is later, call doGet()
                                // Round down to the nearest second for a proper compare
                                // A ifModifiedSince of -1 will always be less
              	    maybeSetLastModified(resp, lastModified)
              	    handleGet(req, resp)
            	    }
                  else
              	    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED)
                }
              }),
              HttpRequestHandler.MethodHead -> ((req, resp) => {
                  val lastModified = getLastModified(req)
                  maybeSetLastModified(resp, lastModified)
                  handleHead(req, resp)
              }),
              HttpRequestHandler.MethodPost -> handlePost,
              HttpRequestHandler.MethodPut -> handlePut,
              HttpRequestHandler.MethodDelete -> handleDelete,
              HttpRequestHandler.MethodOptions -> handleOptions,
              HttpRequestHandler.MethodTrace -> handleTrace
            )
    	    val method = req.getMethod

    	    if (methodMap contains method) methodMap(method)(req, resp)
    	    else
              //
              // Note that this means NO HTTP servlet supports whatever
              // method was requested, anywhere on this server.
              //
              resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,
                  MessageFormat.format(HttpRequestHandler.lStrings.getString(
                      "http.method_not_implemented"), Array[String](method)))
        }
      }
    }
  }
}
