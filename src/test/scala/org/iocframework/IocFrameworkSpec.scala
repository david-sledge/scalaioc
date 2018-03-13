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
  }

  def factoryCalls(factory: Factory) = {
    factory.crackTheWhip("say hello")
    factory.crackTheWhip("say hello once")
    factory.crackTheWhip("hello to...", Map("who" -> "IoC World"))
    factory.crackTheWhip("say hello once again", Map("who" -> "IoC World"))
    factory.crackTheWhip("say hello once again, NOW!")
    factory.crackTheWhip("scoped hello")
    factory.crackTheWhip("scoped hello2")
  }
}
