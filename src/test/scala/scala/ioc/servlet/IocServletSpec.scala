package scala.ioc.servlet

import org.scalatest._
import scala.tools.reflect.ToolBox

import java.net.URL
import java.util.Collections
import java.util.Dictionary
import java.util.Enumeration
import java.util.Hashtable
import javax.servlet._
import scala.servlet.InjectableServletConfig
import scala.servlet.InjectableServletContext
import scala.servlet.InjectableServletResponse
import scala.servlet.InjectableServletRequest

class IocServletSpec extends FlatSpec with Matchers {
  "An IocServlet" should "load the staff.fsp by default" in {

    val iocServlet = new IocServlet
    iocServlet.init(new InjectableServletConfig(new InjectableServletContext(
      getResource = path => new URL(getClass.getClassLoader.getResource("."), path.substring(1))
    ), "test"))

    iocServlet.destroy()

  }

  it should "load a different .fsp when specified" in {

    val parameters = Map(IocServlet.StaffPathParam -> "/WEB-INF/staffTest.fsp")
    val iocServlet = new IocServlet
    iocServlet.init(
        new InjectableServletConfig(new InjectableServletContext(
          getResource = path => new URL(getClass.getClassLoader.getResource("."), path.substring(1)),
        ),
        "test",
        parameters,
      )
    )

    iocServlet.destroy()

  }

  it should "put the manager named 'init' to work on initialization" in {

    val parameters = Map(IocServlet.StaffPathParam -> "/WEB-INF/initTest.fsp")
    val iocServlet = new IocServlet
    iocServlet.init(
        new InjectableServletConfig(new InjectableServletContext(
          getResource = path => new URL(getClass.getClassLoader.getResource("."), path.substring(1)),
        ),
        "test",
        parameters,
      )
    )

    IocServletSpec.writer.toString shouldBe "I have been initialized!"
    IocServletSpec.writer.getBuffer.setLength(0)

    iocServlet.destroy()

  }

  it should "put the manager named 'destroy' to work on finalization" in {

    val parameters = Map(IocServlet.StaffPathParam -> "/WEB-INF/destroyTest.fsp")
    val iocServlet = new IocServlet
    iocServlet.init(
        new InjectableServletConfig(new InjectableServletContext(
          getResource = path => new URL(getClass.getClassLoader.getResource("."), path.substring(1)),
        ),
        "test",
        parameters,
      )
    )
    iocServlet.destroy()

    IocServletSpec.writer.toString shouldBe "I have been destroyed!"
    IocServletSpec.writer.getBuffer.setLength(0)

  }

  it should "make the ServletConfig object available to the init and destroy managers" in {

    val parameters = Map(IocServlet.StaffPathParam -> "/WEB-INF/configTest.fsp")
    val iocServlet = new IocServlet
    iocServlet.init(
        new InjectableServletConfig(new InjectableServletContext(
          getResource = path => new URL(getClass.getClassLoader.getResource("."), path.substring(1)),
        ),
        "test",
        parameters,
      )
    )

    iocServlet.destroy()

  }

  it should "complain if a manager named 'requestHandler' is not available" in {

    val parameters = Map(IocServlet.StaffPathParam -> "/WEB-INF/noHandlerTest.fsp")
    val iocServlet = new IocServlet
    an [ServletException] should be thrownBy iocServlet.init(
        new InjectableServletConfig(new InjectableServletContext(
          getResource = path => new URL(getClass.getClassLoader.getResource("."), path.substring(1)),
        ),
        "test",
        parameters,
      )
    )

    iocServlet.destroy()

  }

  it should "pass a request on to the manager named 'requestHandler'" in {

    val parameters = Map(IocServlet.StaffPathParam -> "/WEB-INF/handlerTest.fsp")
    val iocServlet = new IocServlet
    iocServlet.init(
        new InjectableServletConfig(new InjectableServletContext(
          getResource = path => new URL(getClass.getClassLoader.getResource("."), path.substring(1)),
        ),
        "test",
        parameters,
      )
    )

    iocServlet.service(new InjectableServletRequest, new InjectableServletResponse)

    iocServlet.destroy()

  }

}

object IocServletSpec {
  import java.io.StringWriter

  val writer = new StringWriter

}
