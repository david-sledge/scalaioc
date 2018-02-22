package scala.ioc

import org.scalatest._
import scala.collection._

class FactorySpec extends FlatSpec with Matchers {
  "A factory" should "have managers" in {
    val factory = new Factory()
    val worker = (c: Map[Any, Any]) => "I'm a manager"
    factory.setManager("manager", worker)
    factory.hasManager("manager") should be (true)
    factory.getManager("manager").getClass() should be (worker.getClass())
  }

  it should "note who's on roll" in {
    val factory = new Factory()
    val worker = (c: Map[Any, Any]) => "I'm a manager"
    factory.setManager("manager", worker)
    factory.getManagerIds() should be (Set("manager"))
  }

  "Lazy manager" should "produce the same thing until forced otherwise" in {
    val factory = new Factory()
    factory.setLazyManager("lazyManager", (c: Map[Any, Any]) => s"I haven't worked since ${java.util.Calendar.getInstance()}")
    val result = factory.putToWork("lazyManager", Map())
    result should be (factory.getCachedResult("lazyManager", Map()))
    result should not be (factory.putToWork("lazyManager", Map()))
  }

  "The companion Factory object" should "allow construction without the \"new\" keyword" in {
    val factory = Factory()
    factory shouldBe (a [Factory])
  }
}