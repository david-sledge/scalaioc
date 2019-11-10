package scala.ioc.servlet

  import ppm._
import scala.ppm._
import scala.jdk.CollectionConverters._
import scala.ioc._
  import Factory._
import scala.ioc.ppm._
import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

/**
 * @author David M. Sledge
 */
class IocServlet extends GenericServlet {
  import IocServlet._

  private val serialVersionUID = 797867274650863128L

  // results
  private var factory: Option[Factory] = None

  override def init() = {
    val paramNames: List[String] = getInitParameterNames.asScala.toList
    val preprocessor = Preprocessor()
    val ctx = getServletContext
    preprocessor.addMacro(Some("scalaioc.servlet"), Some("embed"), embedImpl(ctx))
    val staffPath = if (paramNames contains StaffPathParam)
        getInitParameter(StaffPathParam)
      else DefaultStaffPath
    val stream = Option(ctx.getResourceAsStream(staffPath)) match {
      case Some(stream) => stream
      case _ => throw new ServletException(s"No resource found at the path '$staffPath'")
    }
    // get the path to the fsp
    val (factory, _) = staffFactoryFromStream(
        stream,
        preprocessor = preprocessor,
        encoding =
          if (paramNames contains StaffEncodingParam)
            getInitParameter(StaffEncodingParam)
          else DefaultStaffEncoding,// */
        src = Some(DefaultStaffPath),
      )
    this.factory = Some(factory)

    // make sure the worker exists
    if (!factory.hasManager(HandlerName))
      throw new ServletException(s"Cannot find the worker named '$HandlerName' to handle HTTP requests")

    if (factory.hasManager(InitializerKey)) {
      factory.putToWork(InitializerKey,
          Map(ServletConfigKey -> getServletConfig))
    }

  }

  override def destroy() =
    if (factory.get.hasManager(DestroyerKey)) {
      factory.get.putToWork(DestroyerKey,
          Map(ServletConfigKey -> getServletConfig))
    }

  /*
   * (non-Javadoc)
   * @see javax.servlet.GenericServlet#service(javax.servlet.ServletRequest,
   * javax.servlet.ServletResponse)
   */
  override def service(req: ServletRequest, resp: ServletResponse) = {
    factory.get.putToWork(
      HandlerName,
      Map(
        RequestKey -> req,
        ResponseKey -> resp,
      )
    )
  }
}

object IocServlet {
  // Parameter names
  val StaffPathParam = "staffPath"
  val StaffEncodingParam = "enc"

  // defaults
  val DefaultStaffPath = "/WEB-INF/staff.fsp"
  val DefaultStaffEncoding = "utf-8"

  // manager keys and IoC context keys
  val HandlerName = "requestHandler"
  val RequestKey = "req"
  val ResponseKey = "resp"
  val ServletConfigKey = "servletConfig"
  val InitializerKey = "init"
  val DestroyerKey = "destroy"
}
