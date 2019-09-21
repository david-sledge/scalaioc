package scala.ioc.servlet

import org.scalatest._
import scala.tools.reflect.ToolBox

import java.net.URL
import java.util.Collections
import java.util.Dictionary
import java.util.Enumeration
import java.util.Hashtable
import javax.servlet._

class IocServletSpec extends FlatSpec with Matchers {
  "An IocServlet" should "load the staff.fsp by default" in {

    val iocServlet = new IocServlet
    println(s"System resource: ${getClass.getClassLoader.getResource(".")}")
    iocServlet.init(new InjectableServletConfig(new InjectableServletContext(
      getResource = path => new URL(getClass.getClassLoader.getResource("."), path.substring(1))
    ), "test"))

  }
}
