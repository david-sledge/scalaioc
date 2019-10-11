package scala.servlet

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

package object http {

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
  val LocalStrings = ResourceBundle.getBundle(LocalStringsFile)

  val NotAllowedHandler = (lStringName: String) =>
    (req: HttpServletRequest, resp: HttpServletResponse) => {
      val msg = LocalStrings.getString(lStringName);

      if (req.getProtocol.endsWith("1.1"))
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
      else
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }

  val DefaultDeleteHandler = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_delete_not_supported")(req, resp)

  val DefaultGetHandler = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_get_not_supported")(req, resp)

  val DefaultPostHandler = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_post_not_supported")(req, resp)

  val DefaultPutHandler = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_put_not_supported")(req, resp)

  def createRequestHandler(
    handleGet: (HttpServletRequest, HttpServletResponse) =>
      Unit = DefaultGetHandler,
    handlePost: (HttpServletRequest, HttpServletResponse) =>
      Unit = DefaultPostHandler,
    handleDelete: (HttpServletRequest, HttpServletResponse) =>
      Unit = DefaultDeleteHandler,
    handlePut: (HttpServletRequest, HttpServletResponse) =>
      Unit = DefaultPutHandler,
    methodMap: Map[String, (HttpServletRequest, HttpServletResponse) => Unit] = Map.empty,
    getLastModified: (HttpServletRequest) => Long = (req) => -1,
  ) = {

    (req: HttpServletRequest, resp: HttpServletResponse) => {

      def maybeSetLastModified(resp: HttpServletResponse, lastModified: Long) = {
        if (!resp.containsHeader(HeaderLastMod) && lastModified >= 0)
          resp.setDateHeader(HeaderLastMod, lastModified);
      }

      req.getMethod match {

        case MethodGet => {
          val lastModified = getLastModified(req)

          if (lastModified == -1)
            // servlet doesn't support if-modified-since, no reason
            // to go through further expensive logic
            handleGet(req, resp)
          else {
            val ifModifiedSince = req.getDateHeader(HeaderIfModSince);

            if (ifModifiedSince < lastModified) {
              // If the mod time is later, call handleGet()
              // Round down to the nearest second for a proper compare
              // A ifModifiedSince of -1 will always be less
              maybeSetLastModified(resp, lastModified)
              handleGet(req, resp)
            }
            else
              resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED)
          }
        }

        case MethodHead => {

          val lastModified = getLastModified(req)
          maybeSetLastModified(resp, lastModified)

          if (handleGet == DefaultGetHandler) {

            val msg = "HTTP method HEAD is not supported by this URL because GET is also not supported"

            if (req.getProtocol.endsWith("1.1"))
              resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
            else
              resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);

          }
          else {

            val response = new HttpServletResponseWrapper(resp) {

              val noBody = new ServletOutputStream {

                private var contentLength = 0

                def getContentLength: Int = contentLength

                def write(b: Int) = contentLength += 1

                override def write(buf: Array[Byte], offset: Int, len: Int) =
                {

                  if (len >= 0)
                    contentLength += len
                  else
                    throw new IllegalArgumentException(
                      LocalStrings.getString("err.io.negativelength")
                    )
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

        case MethodPost => handlePost(req, resp)

        case MethodPut => handlePut(req, resp)

        case MethodDelete => handleDelete(req, resp)

        case MethodOptions => {

          def template(handler: (HttpServletRequest, HttpServletResponse) => Unit,
              defaultHandler: (HttpServletRequest, HttpServletResponse) => Unit,
              method: String) =
            (allow: List[String]) =>
              if (handler == defaultHandler) allow else method :: allow

          resp.setHeader(
            "Allow",
            template(
              handleDelete,
              DefaultDeleteHandler,
              MethodDelete
            )(
              (
                (allow: List[String]) =>
                  if (handleGet == DefaultGetHandler)
                    allow
                  else
                    MethodGet :: MethodHead :: allow
              )(
                template(
                  handlePost,
                  DefaultPostHandler,
                  MethodPost,
                )(
                  template(
                    handlePut,
                    DefaultPutHandler,
                    MethodPut,
                  )(
                    List[String](
                      MethodOptions,
                      MethodTrace,
                    )
                  )
                )
              )
            ).mkString(", ")
          )
        }

        case MethodTrace => {

          val CRLF = "\r\n"
          val response =
            ("TRACE " :: req.getRequestURI :: " " :: req.getProtocol ::
              req.getHeaderNames.asScala.toList.foldLeft(List(CRLF))((acc, s) => {
                  CRLF :: s :: ": " :: req.getHeader(s) :: acc
                })).mkString
          resp.setContentType("message/http")
          resp.setContentLength(response.length)
          resp.getOutputStream.print(response)

        }

        case method @ _ => {

          methodMap.get(method) match {
            case Some(handler) => handler(req, resp)
            case _ => 
              //
              // Other HTTP servlets in the container may support the requested
              // method, but this one does not.
              //
              resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                  MessageFormat.format(LocalStrings.getString(
                      "http.method_not_implemented"), method))
          }

        }
      }

    }
  }
}