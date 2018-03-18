package scala.ioc.servlet

import scala.collection.JavaConverters._
import org.iocframework.staffFactoryFromStream
import scala.ioc._
import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse;

/**
 * @author David M. Sledge
 */
class IocServlet extends GenericServlet {
  private val serialVersionUID = 797867274650863128L

  // results
  private var factory: Factory = _
  private var handlerName: String = _
  private var initializerName: String = _
  private var destroyerName: String = _
  private var reqName: String = _
  private var respName: String = _
  private var servletConfigName: String = _

  /**
   * Initialization of the servlet. <br>
   * 
   * @throws ServletException if an error occured
   */
  override def init() = {
    val paramNames: List[String] = getInitParameterNames.asScala.toList
    // get the path to the sfs
    val staffPath =
      if (paramNames contains IocServlet.StaffPathParam)
        getInitParameter(IocServlet.StaffPathParam)
      else IocServlet.DefaultStaffPath
    val stream = getServletContext.getResourceAsStream(staffPath)
    val (factory, _) = staffFactoryFromStream(stream)

    // get the name of the worker to handle HTTP requests
    handlerName =
      if (paramNames.contains(IocServlet.HandlerParam))
        getInitParameter(IocServlet.HandlerParam)
      else IocServlet.DefaultHandlerName

    // make sure the worker exists
    if (!factory.hasManager(handlerName))
      throw new ServletException("Cannot find the worker named '"
          + handlerName + "' to handle HTTP requests")

    // optional worker to perform initializing procedures
    initializerName = getInitParameter(IocServlet.InitializerParam)

    if (initializerName != null && !factory.hasManager(initializerName))
    {
      log("no factory worker by the name of '" + initializerName
          + "' found.")
      initializerName = null
    }

    // optional worker to perform shutdown procedures
    destroyerName = getInitParameter(IocServlet.DestroyerParam);

    if (destroyerName != null && !factory.hasManager(destroyerName))
    {
      log("no factory worker by the name of '" + destroyerName + "' found.")
      destroyerName = null
    }

    // key for local request object
    reqName =
      if (paramNames.contains(IocServlet.RequestNameParam))
        getInitParameter(IocServlet.RequestNameParam)
      else IocServlet.DefaultRequestKey
    // key for local response object
    respName =
      if (paramNames.contains(IocServlet.ResponseNameParam))
        getInitParameter(IocServlet.ResponseNameParam)
      else IocServlet.DefaultResponseKey
    // key for local servletConfig object
    servletConfigName =
      if (paramNames.contains(IocServlet.ServletConfigNameParam))
        getInitParameter(IocServlet.ServletConfigNameParam)
      else IocServlet.DefaultServletConfig

    if (initializerName != null)
    {
      factory.putToWork(initializerName,
          Map(servletConfigName -> getServletConfig()))
    }
  }

  override def destroy() =
    if (destroyerName != null)
      factory.putToWork(destroyerName,
          Map(servletConfigName -> getServletConfig()))

  /*
   * (non-Javadoc)
   * @see javax.servlet.GenericServlet#service(javax.servlet.ServletRequest,
   * javax.servlet.ServletResponse)
   */
  override def service(req: ServletRequest, resp: ServletResponse) =
    factory.putToWork(handlerName, Map(reqName -> req, respName -> resp))
}

object IocServlet {
  // Parameters
  val StaffPathParam = "staffPath"
  val HandlerParam = "handlerName"
  val InitializerParam = "initializerName"
  val DestroyerParam = "destroyerName"
  val RequestNameParam = "reqName"
  val ResponseNameParam = "respName"
  val ServletConfigNameParam = "servletConfigName"

  // defaults
  val DefaultStaffPath = "WEB-INF/staff.sfs"
  val DefaultHandlerName = "requestHandler"
  val DefaultRequestKey = "req"
  val DefaultResponseKey = "resp"
  val DefaultServletConfig = "servletConfig"
}
