package scala.ioc.ppm

import scala.ioc._
import org.scalatest._

class PackageSpec extends FlatSpec with Matchers {

  "Staffing a factory" should "populate it with workers" in {
    val conf = """
`namespace|scala.ioc`

"id" `#=` "worker"
"id".`#=`("worker")
"id" `#=` (worker = "worker")
"id".`#=`(worker = "worker")
"id" `#=>` "worker"
`#def`(None, {
  import scala.collection.immutable._
  import scala.reflect.runtime.universe._

  (namespaceName: Option[String], localName: Option[String]) => (expr: Option[Tree], args: Seq[Tree]) => {
    import scala.reflect.runtime.universe._
    Right(q"()")
  }
})
"""
    val (factory, preprocessor) = staffFactory(conf)
    preprocessor.hasMacro(None, "") shouldBe true
    preprocessor.hasOwnMacro(None, "") shouldBe false
    val (fFactory, _) = staffFactoryFromFile("src/test/resources/staff.fsp")
    factoryCalls(fFactory)
    fFactory.putToWork("requestHandler", Map()) shouldBe "I'll handle it"
    val (ffFactory, _) = staffFactoryFromResource("staff.fsp")
    factoryCalls(ffFactory)
    /*
    */
  }

  def factoryCalls(factory: Factory) = {
    factory.putToWork("say hello")
    factory.putToWork("say hello once")
    factory.putToWork("hello to...", Map("who" -> "IoC World"))
    factory.putToWork("say hello once again", Map("who" -> "IoC World"))
    factory.putToWork("say hello once again, NOW!")
    factory.putToWork("scoped hello")
    factory.putToWork("scoped hello2")
    factory.putToWork("promotion")
    factory.hasManager("prop") should be (true)
    factory.putToWork("propTest") should be (factory.putToWork("promotion"))
  }
}
