package scala.ioc

import scala.ioc.xml._
import org.scalatest._
import scala.meta._

class StaffingSpec extends FlatSpec with Matchers {
  "The IoC transformer" should "manipulate the scala AST" in {
    val staffing = Staffing()
    staffing.transformIoC(q"""`#scala.ioc#singleton`("id", "value")""").structure should be
      q"""factory.setLazyManager("id", (c: Map[Any, Any]) => "value")""".structure
  }

  "A staffing object" should "hire recruiters" in {
    val staffing = Staffing()
    staffing.addRecruiter("scala.xml", "xml", postJobXml)
    val xformed = staffing.transformIoC(q"""`#scala.xml#xml`("utf-8", "1.0")()""")
    println(staffing.transformIoC(q"""`#scala.xml#xml`("utf-8", "1.0")()"""))
//    staffing.transformIoC(q"""`#scala.xml#xml`("utf-8", "1.0")()""").structure shouldBe q"""    
//val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))
//typeclass.writeDocumentStart(typeclass.cast(writer), "utf-8", "1.0")
//((c: Map[Any, Any]) => {})(c)
//typeclass.writeEndDocument(typeclass.cast(writer))
//"""
  }
}