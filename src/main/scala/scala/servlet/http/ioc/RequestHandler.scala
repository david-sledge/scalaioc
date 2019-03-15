package scala.servlet.http.ioc

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls
import scala.collection.immutable.Map
import scala.collection.immutable.Seq
import scala.servlet.http.RequestHandler._

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

object RequestHandler {

  def apply(
    options: Seq[String] = Seq(),
    deleteHandler: Option[Map[Any, Any] => Unit] = None,
    getHandler: Option[Map[Any, Any] => Unit] = None,
    postHandler: Option[Map[Any, Any] => Unit] = None,
    putHandler: Option[Map[Any, Any] => Unit] = None,
    headHandler: Option[Map[Any, Any] => Unit] = None,
    traceHandler: Option[Map[Any, Any] => Unit] = None,
    getLastModified: (HttpServletRequest) => Long = (req) => -1,
    reqKey: String = "req",
    respKey: String = "resp"
  ) = {

    def getReq(c: Map[Any, Any]) = (c.get(reqKey).get.asInstanceOf[HttpServletRequest])

    def getResp(c: Map[Any, Any]) = (c.get(respKey).get.asInstanceOf[HttpServletResponse])

    val handleDelete =
      deleteHandler match {
      case Some(handler) => handler
      case _ => (c: Map[Any, Any]) => {
        DefaultDeleteHandler(
            getReq(c),
            getResp(c))
      }
    }

    val handleGet =
      getHandler match {
      case Some(handler) => {
        handler
      }
      case _ => (c: Map[Any, Any]) => {
        DefaultGetHandler(
          getReq(c),
          getResp(c))
      }
    }

    val handleOptions =
      (c: Map[Any, Any]) => {
        val resp = getResp(c)
        resp.setHeader("Allow", options.mkString(", "))
      }

    val handlePost =
      postHandler match {
      case Some(handler) => handler
      case _ => (c: Map[Any, Any]) => {
        DefaultPostHandler(
          getReq(c),
          getResp(c))
      }
    }

    val handlePut =
      putHandler match {
      case Some(handler) => handler
      case _ => (c: Map[Any, Any]) => {
        DefaultPutHandler(
          getReq(c),
          getResp(c))
      }
    }

    val handleHead =
      headHandler match {
        case Some(handleHead) => handleHead
        case _ =>
        (c: Map[Any, Any]) =>
          DefaultOptionsHandler(
            (req, resp) => {
              handleDelete(c + (reqKey -> req) + (respKey -> resp))
            }
          , (req, resp) => {
              handleGet(c + (reqKey -> req) + (respKey -> resp))
            }
          , (req, resp) => {
              handlePost(c + (reqKey -> req) + (respKey -> resp))
            }
          , (req, resp) => {
              handlePut(c + (reqKey -> req) + (respKey -> resp))
            }
          )
    }

    val handleTrace =
      putHandler match {
      case Some(handler) => handler
      case _ => (c: Map[Any, Any]) => {
        DefaultTraceHandler(
          getReq(c),
          getResp(c))
      }
    }

    (c: Map[Any, Any]) => DefaultHandler(
        (req, resp) => {
          handleDelete(c + (reqKey -> req) + (respKey -> resp))
        }
      , (req, resp) => {
          handleGet(c + (reqKey -> req) + (respKey -> resp))
        }
      , (req, resp) => {
          handleHead(c + (reqKey -> req) + (respKey -> resp))
        }
      , (req, resp) => {
          handleOptions(c + (reqKey -> req) + (respKey -> resp))
        }
      , (req, resp) => {
          handlePost(c + (reqKey -> req) + (respKey -> resp))
        }
      , (req, resp) => {
          handlePut(c + (reqKey -> req) + (respKey -> resp))
        }
      , (req, resp) => {
          handleTrace(c + (reqKey -> req) + (respKey -> resp))
        }
      , getLastModified)(getReq(c), getResp(c))
  }
}
