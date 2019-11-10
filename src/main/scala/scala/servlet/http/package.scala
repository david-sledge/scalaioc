package scala.servlet

import scala.jdk.CollectionConverters._
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

  def handleDeleteDefault[T](t: T)(implicit getTransaction: GetHttpServletTransaction[T]) =
    NotAllowedHandler("http.method_delete_not_supported")(
      getTransaction.getRequest(t),
      getTransaction.getResponse(t),
    )

  def handleGetDefault[T](t: T)(implicit getTransaction: GetHttpServletTransaction[T]) =
    NotAllowedHandler("http.method_get_not_supported")(
      getTransaction.getRequest(t),
      getTransaction.getResponse(t),
    )

  def handlePostDefault[T](t: T)(implicit getTransaction: GetHttpServletTransaction[T]) =
    NotAllowedHandler("http.method_post_not_supported")(
      getTransaction.getRequest(t),
      getTransaction.getResponse(t),
    )

  def handlePutDefault[T](t: T)(implicit getTransaction: GetHttpServletTransaction[T]) =
    NotAllowedHandler("http.method_put_not_supported")(
      getTransaction.getRequest(t),
      getTransaction.getResponse(t),
    )

  val DefaultDeleteHandler = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_delete_not_supported")(req, resp)

  val DefaultGetHandler = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_get_not_supported")(req, resp)

  val DefaultPostHandler = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_post_not_supported")(req, resp)

  val DefaultPutHandler = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_put_not_supported")(req, resp)

  def maybeSetLastModified(resp: HttpServletResponse, lastModified: Long) = {
    if (!resp.containsHeader(HeaderLastMod) && lastModified >= 0)
      resp.setDateHeader(HeaderLastMod, lastModified);
  }

  def template(handler: (HttpServletRequest, HttpServletResponse) => Unit,
      defaultHandler: (HttpServletRequest, HttpServletResponse) => Unit,
      method: String) =
    (allow: Set[String]) =>
      if (handler == defaultHandler) allow else allow + method

  def createRequestHandler[T](
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
  )(implicit getTransaction: GetHttpServletTransaction[T]): T => Unit = {

    val handleHead = (req: HttpServletRequest, resp: HttpServletResponse) => {

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

    val allowHeaderResponse = template(
        handleDelete,
        DefaultDeleteHandler,
        MethodDelete
      )(
        (
          (allow: Set[String]) =>
            if (handleGet == DefaultGetHandler)
              allow
            else
              allow ++ Set(MethodGet, MethodHead)
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
              methodMap.keySet.diff(Set(
                  MethodDelete,
                  MethodGet,
                  MethodHead,
                  MethodOptions,
                  MethodPost,
                  MethodPut,
                  MethodTrace,
              )) ++ Set(
                MethodOptions,
                MethodTrace,
              )
            )
          )
        )
      ).mkString(", ")

    (t: T) => {
      val req = getTransaction.getRequest(t)
      val resp = getTransaction.getResponse(t)

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

        case MethodHead => handleHead(req, resp)

        case MethodPost => handlePost(req, resp)

        case MethodPut => handlePut(req, resp)

        case MethodDelete => handleDelete(req, resp)

        case MethodOptions => {

          resp.setHeader(
            "Allow",
            allowHeaderResponse,
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
            case Some(handleMethod) => handleMethod(req, resp)
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

  def template2[T](handler: Option[T => Unit],
      method: String) =
    (allow: Set[String]) =>
      if (handler == None) allow else allow + method

  def createRequestHandler2(
    getHandler: Option[Map[Any, Any] => Unit] = None,
    postHandler: Option[Map[Any, Any] => Unit] = None,
    deleteHandler: Option[Map[Any, Any] => Unit] = None,
    putHandler: Option[Map[Any, Any] => Unit] = None,
    methodMap: Map[String, Map[Any, Any] => Unit] = Map.empty,
    getLastModified: (Map[Any, Any]) => Long = (t: Map[Any, Any]) => -1,
  )(implicit getTransaction: GetHttpServletTransaction[Map[Any, Any]]): Map[Any, Any] => Unit = {

    val handleGet = getHandler.getOrElse(
      (t: Map[Any, Any]) => handleGetDefault(t)(getTransaction)
    )

    val handlePost = postHandler.getOrElse(
      (t: Map[Any, Any]) => handlePostDefault(t)(getTransaction)
    )

    val handleDelete = deleteHandler.getOrElse(
      (t: Map[Any, Any]) => handleDeleteDefault(t)(getTransaction)
    )

    val handlePut = putHandler.getOrElse(
      (t: Map[Any, Any]) => handlePutDefault(t)(getTransaction)
    )

    val handleHead = (t: Map[Any, Any]) => {

      val req = getTransaction.getRequest(t)
      val resp = getTransaction.getResponse(t)

      val lastModified = getLastModified(t)
      maybeSetLastModified(resp, lastModified)

      getHandler match {
        case Some(handle) => {
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

          handle(getTransaction.setResponse(t, response))
          response.setContentLength
        }

        case _ => {

          val msg = "HTTP method HEAD is not supported by this URL because GET is also not supported"

          if (req.getProtocol.endsWith("1.1"))
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
          else
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);

        }

      }

    }

    val allowHeaderResponse = template2(
        deleteHandler,
        MethodDelete
      )(
        (
          (allow: Set[String]) =>
            if (getHandler == None)
              allow
            else
              allow ++ Set(MethodGet, MethodHead)
        )(
          template2(
            postHandler,
            MethodPost,
          )(
            template2(
              putHandler,
              MethodPut,
            )(
              methodMap.keySet.diff(Set(
                  MethodDelete,
                  MethodGet,
                  MethodHead,
                  MethodOptions,
                  MethodPost,
                  MethodPut,
                  MethodTrace,
              )) ++ Set(
                MethodOptions,
                MethodTrace,
              )
            )
          )
        )
      ).mkString(", ")

    (t: Map[Any, Any]) => {
      val req = getTransaction.getRequest(t)
      val resp = getTransaction.getResponse(t)

      req.getMethod match {

        case MethodGet => {
          val lastModified = getLastModified(t)

          if (lastModified == -1)
            // servlet doesn't support if-modified-since, no reason
            // to go through further expensive logic
            handleGet(t)
          else {
            val ifModifiedSince = req.getDateHeader(HeaderIfModSince);

            if (ifModifiedSince < lastModified) {
              // If the mod time is later, call handleGet()
              // Round down to the nearest second for a proper compare
              // A ifModifiedSince of -1 will always be less
              maybeSetLastModified(resp, lastModified)
              handleGet(t)
            }
            else
              resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED)
          }

        }

        case MethodHead => handleHead(t)

        case MethodPost => handlePost(t)

        case MethodPut => handlePut(t)

        case MethodDelete => handleDelete(t)

        case MethodOptions => {

          resp.setHeader(
            "Allow",
            allowHeaderResponse,
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
            case Some(handleMethod) => handleMethod(t)
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
