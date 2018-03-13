package org.iocframework

import org.scalatest._
import scala.meta._
import org.scalactic.source.Position.apply
import scala.ioc.Factory

class IocFrameworkSpec extends FlatSpec with Matchers {

  "Staffing a factory" should "populate it with workers" in {
    val conf = """
`#scala.ioc#=`("id", "worker")
`#scala.ioc#=`(id = "id", "worker")
`#scala.ioc#=`("worker", id = "id")
`#scala.ioc#=`(id = "id", worker = "worker")
`#scala.ioc#=`(worker = "worker", id = "id")
`#scala.ioc#=`("id", worker = "worker")
`#scala.ioc#=`(worker = "worker", "id")
"id" `#scala.ioc#=>` "worker"
"""
    val (factory, _) = staffFactory(conf)
    val (fFactory, _) = staffFactoryFromFile("src/test/resources/FactoryStaff.sfs")
    factoryCalls(fFactory)
    fFactory.putToWork("requestHandler", Map()) shouldBe "I'll handle it"
    val (ffFactory, _) = staffFactoryFromResource("FactoryStaff.sfs")
    factoryCalls(ffFactory)
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
