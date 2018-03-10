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
    //staffing.addRecruiter("scala.xml", "dtd", postJobDtd)
    staffing.addRecruiter("scala.xml.element", null, postJobElement)
    val xformed = staffing.transformIoC(
        q"""
`#scala.xml#xml`(encoding = "utf-8", version = "1.0"
, dtd = "<!DOCTYPE html>"
, `#scala.xml.element#html`(xmlns="http://www.w3.org/1999/xhtml", lang="en-us"
  , "insert text here", "comments can be inseted, too"
  )
)"""
    )
    println(xformed)
//    println(q""" "literal" """.structure)
    xformed.syntax shouldBe q"""{
  val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))
  typeclass.writeStartDocument(typeclass.cast(writer), "utf-8", "1.0")
  typeclass.writeDTD(typeclass.cast(writer), "<!DOCTYPE html>")
  ((c: Map[Any, Any]) => {
    ((c: Map[Any, Any]) => if (c("cdata") == ()) () else {
      val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))
      typeclass.writeCharacters(typeclass.cast(writer), c("cdata").toString)
    })(c + ("cdata" -> {
      val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))
      typeclass.writeStartElement(typeclass.cast(writer), "html")
      typeclass.writeAttribute(typeclass.cast(writer), "xmlns".toString, "http://www.w3.org/1999/xhtml".toString)
      typeclass.writeAttribute(typeclass.cast(writer), "lang".toString, "en-us".toString)
      ((c: Map[Any, Any]) => {
        ((c: Map[Any, Any]) => if (c("cdata") == ()) () else {
          val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))
          typeclass.writeCharacters(typeclass.cast(writer), c("cdata").toString)
        })(c + ("cdata" -> "insert text here"))
        ((c: Map[Any, Any]) => if (c("cdata") == ()) () else {
          val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))
          typeclass.writeCharacters(typeclass.cast(writer), c("cdata").toString)
        })(c + ("cdata" -> "comments can be inseted, too"))
      })(c)
      typeclass.writeEndElement(typeclass.cast(writer))
    }))
  })(c)
  typeclass.writeEndDocument(typeclass.cast(writer))
}""".syntax
  }
}