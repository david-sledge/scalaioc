package scala.ioc.servlet

  import blm._
import scala.blm._
import scala.collection.JavaConverters._
import scala.ioc._
import scala.ioc.blm._
import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

/**
 * @author David M. Sledge
 */
class IocServlet extends GenericServlet {
  private val serialVersionUID = 797867274650863128L

  // results
  private var factory: Option[Factory] = None
  private var handlerName: String = IocServlet.DefaultHandlerName
  private var initializerName: Option[String] = None
  private var destroyerName: Option[String] = None
  private var reqName: String = IocServlet.DefaultRequestKey
  private var respName: String = IocServlet.DefaultResponseKey
  private var servletConfigName: String = IocServlet.DefaultServletConfigKey

  override def init() = {
    val paramNames: List[String] = getInitParameterNames.asScala.toList
    val preprocessor = Preprocessor()
    val ctx = getServletContext
    preprocessor.addMacro(Some("scala.servlet"), Some("embed"), embedImpl(ctx))
    // get the path to the sfs
    val (factory, _) = staffFactoryFromStream(
        ctx.getResourceAsStream(
          if (paramNames contains IocServlet.StaffPathParam)
            getInitParameter(IocServlet.StaffPathParam)
          else IocServlet.DefaultStaffPath
        ),
        preprocessor = preprocessor,
        encoding =
          if (paramNames contains IocServlet.EncodingParam)
            getInitParameter(IocServlet.EncodingParam)
          else IocServlet.DefaultEncoding,// */
        src = Some(IocServlet.DefaultStaffPath)
      )
    this.factory = Some(factory)

    // get the name of the worker to handle HTTP requests
    if (paramNames.contains(IocServlet.HandlerParam))
      handlerName = Option(getInitParameter(IocServlet.HandlerParam)) match {
        case Some(name) => name
        case None => IocServlet.DefaultHandlerName
      }

    // make sure the worker exists
    if (!factory.hasManager(handlerName))
      throw new ServletException("Cannot find the worker named '"
          + handlerName + "' to handle HTTP requests")

    // optional worker to perform initializing procedures
    initializerName = Option(getInitParameter(IocServlet.InitializerParam)) match {
      case n @ Some(name) => if (!factory.hasManager(name))
        {
          log("no factory worker by the name of '" + name + "' found.")
          None
        }
        else n
      case _ => None
    }

    // optional worker to perform shutdown procedures
    destroyerName = Option(getInitParameter(IocServlet.DestroyerParam)) match {
      case n @ Some(name) => if (!factory.hasManager(name))
        {
          log("no factory worker by the name of '" + name + "' found.")
          None
        }
        else n
      case _ => None
    }

    // key for local request object
    if (paramNames.contains(IocServlet.RequestNameParam))
      reqName = getInitParameter(IocServlet.RequestNameParam)

    // key for local response object
    if (paramNames.contains(IocServlet.ResponseNameParam))
      respName = getInitParameter(IocServlet.ResponseNameParam)

    // key for local servletConfig object
    if (paramNames.contains(IocServlet.ServletConfigNameParam))
      servletConfigName = getInitParameter(IocServlet.ServletConfigNameParam)

    if (initializerName != None)
    {
      factory.putToWork(initializerName.get,
          Map(servletConfigName -> getServletConfig))
    }
  }

  override def destroy() =
    if (destroyerName != None)
      factory.get.putToWork(destroyerName.get,
          Map(servletConfigName -> getServletConfig))

  /*
   * (non-Javadoc)
   * @see javax.servlet.GenericServlet#service(javax.servlet.ServletRequest,
   * javax.servlet.ServletResponse)
   */
  override def service(req: ServletRequest, resp: ServletResponse) = {
    factory.get.putToWork(handlerName, Map(reqName -> req, respName -> resp))
  }
}

object IocServlet {
  // Parameters
  val StaffPathParam = "staffPath"
  val EncodingParam = "enc"
  val HandlerParam = "handlerName"
  val InitializerParam = "initializerName"
  val DestroyerParam = "destroyerName"
  val RequestNameParam = "reqName"
  val ResponseNameParam = "respName"
  val ServletConfigNameParam = "servletConfigName"

  // defaults
  val DefaultStaffPath = "/WEB-INF/staff.sfs"
  val DefaultEncoding = "utf-8"
  val DefaultHandlerName = "requestHandler"
  val DefaultRequestKey = "req"
  val DefaultResponseKey = "resp"
  val DefaultServletConfigKey = "servletConfig"
}
