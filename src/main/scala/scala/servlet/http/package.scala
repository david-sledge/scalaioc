package scala.servlet

import java.io.{OutputStreamWriter, PrintWriter}
import java.text.MessageFormat
import java.util.ResourceBundle

import javax.servlet.ServletOutputStream
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpServletResponseWrapper}

import scala.jdk.CollectionConverters._
import scala.language.reflectiveCalls

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
  val LocalStrings: ResourceBundle = ResourceBundle.getBundle(LocalStringsFile)

  val NotAllowedHandler: String => (HttpServletRequest, HttpServletResponse) => Unit = (lStringName: String) =>
    (req: HttpServletRequest, resp: HttpServletResponse) => {
      val msg = LocalStrings.getString(lStringName)

      if (req.getProtocol.endsWith("1.1"))
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg)
      else
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg)
    }

  def handleDeleteDefault[T](t: T)(implicit getTransaction: HttpServletTransactionTypeClass[T]): Unit =
    NotAllowedHandler("http.method_delete_not_supported")(
      getTransaction.getRequest(t),
      getTransaction.getResponse(t),
    )

  def handleGetDefault[T](t: T)(implicit getTransaction: HttpServletTransactionTypeClass[T]): Unit =
    NotAllowedHandler("http.method_get_not_supported")(
      getTransaction.getRequest(t),
      getTransaction.getResponse(t),
    )

  def handlePostDefault[T](t: T)(implicit getTransaction: HttpServletTransactionTypeClass[T]): Unit =
    NotAllowedHandler("http.method_post_not_supported")(
      getTransaction.getRequest(t),
      getTransaction.getResponse(t),
    )

  def handlePutDefault[T](t: T)(implicit getTransaction: HttpServletTransactionTypeClass[T]): Unit =
    NotAllowedHandler("http.method_put_not_supported")(
      getTransaction.getRequest(t),
      getTransaction.getResponse(t),
    )

  val DefaultDeleteHandler: (HttpServletRequest, HttpServletResponse) => Unit = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_delete_not_supported")(req, resp)

  val DefaultGetHandler: (HttpServletRequest, HttpServletResponse) => Unit = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_get_not_supported")(req, resp)

  val DefaultPostHandler: (HttpServletRequest, HttpServletResponse) => Unit = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_post_not_supported")(req, resp)

  val DefaultPutHandler: (HttpServletRequest, HttpServletResponse) => Unit = (req: HttpServletRequest, resp: HttpServletResponse) =>
    NotAllowedHandler("http.method_put_not_supported")(req, resp)

  def maybeSetLastModified(resp: HttpServletResponse, lastModified: Long): Unit = {
    if (!resp.containsHeader(HeaderLastMod) && lastModified >= 0)
      resp.setDateHeader(HeaderLastMod, lastModified)
  }

  def template[T](handler: Option[T => Unit],
      method: String): Set[String] => Set[String] =
    (allow: Set[String]) =>
      if (handler.isEmpty) allow else allow + method

  def createRequestHandler(
                            methodMap: Map[String, Map[Any, Any] => Unit] = Map.empty,
                            getLastModified: Map[Any, Any] => Long = (_: Map[Any, Any]) => -1,
  )(
    implicit getTransaction: HttpServletTransactionTypeClass[Map[Any, Any]]
  ): Map[Any, Any] => Unit = {

    val getHandler = methodMap.get("GET")
    val postHandler = methodMap.get("POST")
    val deleteHandler = methodMap.get("DELETE")
    val putHandler = methodMap.get("PUT")

    val handleHead = (t: Map[Any, Any]) => {

      val req = getTransaction.getRequest(t)
      val resp = getTransaction.getResponse(t)

      val lastModified = getLastModified(t)
      maybeSetLastModified(resp, lastModified)

      getHandler match {
        case Some(handle) =>
          val response = new HttpServletResponseWrapper(resp) {

            val noBody = new ServletOutputStream {

              private var contentLength = 0

              def getContentLength: Int = contentLength

              def write(b: Int): Unit = contentLength += 1

              override def write(buf: Array[Byte], offset: Int, len: Int): Unit =
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

            def setContentLength(): Unit =
              if (!didSetContentLength) {
                writer.flush()

                setContentLength(noBody.getContentLength)
              }

            override def setContentLength(len: Int): Unit = {
              super.setContentLength(len)
              didSetContentLength = true
            }

            override def getOutputStream: ServletOutputStream = noBody

            override def getWriter: PrintWriter = writer
          }

          handle(getTransaction.setResponse(t, response))
          response.setContentLength()

        case _ =>

          val msg = "HTTP method HEAD is not supported by this URL because GET is also not supported"

          if (req.getProtocol.endsWith("1.1"))
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg)
          else
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg)

      }

    }

    val allowHeaderResponse = template(
        deleteHandler,
        MethodDelete
      )(
        (
          (allow: Set[String]) =>
            if (getHandler.isEmpty)
              allow
            else
              allow ++ Set(MethodGet, MethodHead)
        )(
          template(
            postHandler,
            MethodPost,
          )(
            template(
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

        case MethodGet =>
          val lastModified = getLastModified(t)

          val handleGet = getHandler.getOrElse(
            (t: Map[Any, Any]) => handleGetDefault(t)(getTransaction)
          )

          if (lastModified == -1)
            // servlet doesn't support if-modified-since, no reason
            // to go through further expensive logic
            handleGet(t)
          else {
            val ifModifiedSince = req.getDateHeader(HeaderIfModSince)

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

        case MethodHead => handleHead(t)

        case MethodPost => postHandler.getOrElse(
            (t: Map[Any, Any]) => handlePostDefault(t)(getTransaction)
          )(t)

        case MethodPut => putHandler.getOrElse(
          (t: Map[Any, Any]) => handlePutDefault(t)(getTransaction)
        )(t)

        case MethodDelete => deleteHandler.getOrElse(
            (t: Map[Any, Any]) => handleDeleteDefault(t)(getTransaction)
          )(t)

        case MethodOptions =>

          resp.setHeader(
            "Allow",
            allowHeaderResponse,
          )

        case MethodTrace =>

          val CRLF = "\r\n"
          val response =
            ("TRACE " :: req.getRequestURI :: " " :: req.getProtocol ::
              req.getHeaderNames.asScala.toList.foldLeft(List(CRLF))((acc, s) => {
                  CRLF :: s :: ": " :: req.getHeader(s) :: acc
                })).mkString
          resp.setContentType("message/http")
          resp.setContentLength(response.length)
          resp.getOutputStream.print(response)

        case method @ _ =>

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
