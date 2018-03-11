package org.iocframework

import org.scalatest._
import scala.meta._
import org.scalactic.source.Position.apply
import scala.ioc.Factory

class IocFrameworkSpec extends FlatSpec with Matchers {

  "Staffing a factory" should "populate it with workers" in {
    val conf = """
`#scala.ioc#singleton`("id", "worker")
`#scala.ioc#singleton`(id = "id", "worker")
`#scala.ioc#singleton`("worker", id = "id")
`#scala.ioc#singleton`(id = "id", worker = "worker")
`#scala.ioc#singleton`(worker = "worker", id = "id")
`#scala.ioc#singleton`("id", worker = "worker")
`#scala.ioc#singleton`(worker = "worker", "id")
`#scala.ioc#prototype`("id", "worker")
"""
    val (factory, _) = staffFactory(conf)
    val (fFactory, _) = staffFactoryFromFile("src/test/resources/FactoryStaff.sfs")
    factoryCalls(fFactory)
    fFactory.getCachedResult("requestHandler", Map()) shouldBe "I'll handle it"
  }

  def factoryCalls(factory: Factory) = {
    factory.putToWork("say hello")
    factory.putToWork("say hello once")
    factory.putToWork("hello to...", Map("who" -> "IoC World"))
    factory.putToWork("say hello once again", Map("who" -> "IoC World"))
    factory.putToWork("say hello once again, NOW!")
    factory.putToWork("scoped hello")
    factory.putToWork("scoped hello2")
  }
}
