package scala.ioc.ppm

import scala.ioc._
import org.scalatest._

class packageSpec extends FlatSpec with Matchers {

  "populateStaffingMacros()" should "add the core IoC macros to a preprocessor" in {
    val preprocessor = populateStaffingMacros()
//    println(preprocessor.listMacros)
    preprocessor.listMacros should be (
      Map(
        Some("scalaioc") -> Set(
          Some("="),
          Some("=>"),
          Some("ref"),
          Some("ref!"),
          Some("let"),
          Some("$"),
          Some("def"),
          Some("resource"),
          Some("embed"),
        ),
      )
    )
  }

  "Staffing a factory" should "populate it with workers" in {
    val conf = """
`namespace|scalaioc`

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
    fFactory.putToWork("requestHandler", Map.empty) shouldBe "I'll handle it"
    val (ffFactory, _) = staffFactoryFromResource("staff.fsp")
    factoryCalls(ffFactory)
    /*
    */
  }

  def factoryCalls(factory: Factory) = {
    import java.io.StringWriter
    val writer = new StringWriter
    val ctxMap = Map[Any, Any]("out" -> writer)
    factory.putToWork("say hello", ctxMap) shouldBe (())
    writer.toString shouldBe "No."
    writer.getBuffer.setLength(0)
    factory.putToWork("say hello once", ctxMap) shouldBe (())
    writer.toString shouldBe "Hello, World!  You'll have to force me to say it again."
    writer.getBuffer.setLength(0)
    factory.putToWork("say hello once", ctxMap) shouldBe (())
    writer.toString shouldBe ""
    factory.putToWork("hello to...", Map("who" -> "IoC World")) shouldBe s"Hello, IoC World!"
    factory.putToWork("say hello once again", ctxMap) shouldBe (())
    writer.toString shouldBe ""
    factory.putToWork("say hello once again, NOW!", ctxMap) shouldBe (())
    writer.toString shouldBe "Hello, World!  You'll have to force me to say it again."
    writer.getBuffer.setLength(0)
    factory.putToWork("scoped hello", ctxMap)
    writer.toString shouldBe "Hello, Universe!"
    writer.getBuffer.setLength(0)
    factory.putToWork("scoped hello2", ctxMap) shouldBe "Hello, Universe!"
    writer.toString shouldBe ""
  }
}
