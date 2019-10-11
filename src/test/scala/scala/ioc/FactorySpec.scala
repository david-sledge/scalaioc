package scala.ioc

  import Factory._

import org.scalatest._

class FactorySpec extends FlatSpec with Matchers {
  "A factory" should "have managers" in {
    val factory = Factory()
    val worker = (c: Map[Any, Any]) => "I'm a manager"
    setManager(factory, "manager", worker)
    factory.hasManager("manager") should be (true)
  }

  it should "note who's on roll" in {
    val factory = Factory()
    val worker = (c: Map[Any, Any]) => "I'm a manager"
    setManager(factory, "manager", worker)
    factory.getManagerIds should be (Set("manager"))
  }

  it should "have a method to clear out any cached singletons so that they'll get reloaded with the next call" in {
    val factory = Factory()
    val id = "lazy bastard"
    setLazyManager(factory, id, c => System.currentTimeMillis)
    val result = factory.putToWork(id, Map.empty)
    Thread sleep 2000
    val result2 = factory.putToWork(id, Map.empty)
    result shouldBe result2
    factory.clearCache
    val result3 = factory.putToWork(id, Map.empty)
    result should not be result3
    
  }

  "Lazy manager" should "produce the same thing until forced otherwise" in {
    val factory = Factory()
    setLazyManager(factory, "lazyManager", (c: Map[Any, Any]) => s"I haven't worked since ${java.util.Calendar.getInstance.getTime}")
    val result = factory.putToWork("lazyManager", Map.empty)
    Thread sleep 2000
    result should be (factory.putToWork("lazyManager", Map.empty))
    result should not be (factory.crackTheWhip("lazyManager", Map.empty))
  }

  "In a factory it" should "be possible to fire workers" in {
    val factory = Factory()
    setLazyManager(factory, "Mr. Sloth", c => {})
    setManager(factory, "Mr. Eager", c => {})
    factory.hasManager("Mr. Sloth") shouldBe true
    factory.hasManager("Mr. Eager") shouldBe true
    factory.fireManager("Mr. Sloth")
    factory.hasManager("Mr. Sloth") shouldBe false
    factory.hasManager("Mr. Eager") shouldBe true
    factory.fireEveryone()
    factory.hasManager("Mr. Sloth") shouldBe false
    factory.hasManager("Mr. Eager") shouldBe false
  }
}