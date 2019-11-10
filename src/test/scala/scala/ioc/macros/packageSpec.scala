package scala.ioc.macros

import scala.ioc._
import org.scalatest._
import scala.tools.reflect.ToolBox

class packageSpec extends FlatSpec with Matchers {

  import WishList._

  "Staffing a factory" should "populate it with workers" in {
    val f = (factory: scala.ioc.Factory) => {
      "manager" *=> s"I haven't worked since ${java.util.Calendar.getInstance.getTime}"
      "lazyManager" *= s"I haven't worked since ${java.util.Calendar.getInstance.getTime}"
    }
    val factory = Factory()
    f(factory)

    val result = factory.putToWork("manager")
    val lazyResult = factory.putToWork("lazyManager")
    Thread sleep 2000
    result should not be (factory.putToWork("manager"))
    lazyResult shouldBe factory.putToWork("lazyManager")
    lazyResult should not be (factory.crackTheWhip("lazyManager"))
  }

  it should "work with runtime-compiled code" in {

    val factory = staffFactory(s"""
import scala.ioc.macros._

  import WishList._

"manager" *=> s"I haven't worked since $${java.util.Calendar.getInstance.getTime}"
"lazyManager" *= s"I haven't worked since $${java.util.Calendar.getInstance.getTime}"
""")

    val result = factory.putToWork("manager")
    val lazyResult = factory.putToWork("lazyManager")
    Thread sleep 2000
    result should not be (factory.putToWork("manager"))
    lazyResult shouldBe factory.putToWork("lazyManager")
    lazyResult should not be (factory.crackTheWhip("lazyManager"))
  }

  it should "" in {

    // this was why I didn't use scala macros... sigh.
    an [scala.tools.reflect.ToolBoxError] should be thrownBy (
      staffFactory(s"""
import scala.ioc.macros._

  import WishList._

"manager" *=> s"I haven't worked since $${java.util.Calendar.getInstance.getTime}"
"lazyManager" *= s"I haven't worked since $${java.util.Calendar.getInstance.getTime}"
"test1" *=> (ref("manager") != ref("manager"))
""")
    )

//    factory.putToWork("test1") shouldBe true
//    val result = factory.putToWork("manager")
//    val lazyResult = factory.putToWork("lazyManager")
//    Thread sleep 2000
//    lazyResult shouldBe factory.putToWork("lazyManager")
//    lazyResult should not be (factory.crackTheWhip("lazyManager"))
  }

}
