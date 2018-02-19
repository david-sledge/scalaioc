package scala.ioc

import scala.collection._
import org.iocframework._
import org.scalatest._
import scala.tools.reflect.ToolBox
import scala.meta._

class Spec extends FlatSpec with Matchers {

  "Something" should "do something else" in {
//    val q"$expr(...$args)" = q"name(argOrd, param = argNamed)"
//    println(args.flatten.structure)
//    println(expr.structure)
//    println(q"name".structure)
//    println(q"name = value".structure)
    val conf = """
import scala.ioc._

singleton("id", "worker")
singleton(id = "id", "worker")
singleton("worker", id = "id")
singleton(id = "id", worker = "worker")
singleton(worker = "worker", id = "id")
singleton("id", worker = "worker")
singleton(worker = "worker", "id")
prototype("id", "worker")

prototype("say hello", println("Hello, World!"))

singleton("say hello once", println("Hello, World!  You'll have to force me to say it again."))

prototype("hello to...", println(s"Hello, ${c("who")}!"))

prototype("say hello once again", ref("say hello once"))

prototype("say hello once again, NOW!", reloadRef("say hello once"))

prototype("scope", "World")

prototype("scoped hello", println(s"Hello, ${ref("scope")}!"))

prototype("scoped hello2", let("who", ref("scope"), ref("hello to...")))
"""
    val (factory, _) = staffFactory(conf)
    factoryCalls(factory)
    val (fFactory, _) = staffFactoryFromFile("src/test/resources/FactoryStaff.sfs")
    factoryCalls(fFactory)
    println(fFactory.getCachedResult("requestHandler", Map()))
  }

  def factoryCalls(factory: Factory) = {
    factory.putToWork("say hello", Map())
    factory.putToWork("say hello once", Map())
    factory.putToWork("hello to...", Map("who" -> "IoC World"))
    factory.putToWork("say hello once again", Map("who" -> "IoC World"))
    factory.putToWork("say hello once again, NOW!", Map())
    factory.putToWork("scoped hello", Map())
    factory.putToWork("scoped hello2", Map())
  }
}
