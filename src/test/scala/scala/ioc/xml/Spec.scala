package scala.ioc

import org.scalatest._
import scala.ioc.xml._
import scala.xml.stream._
import scala.meta._
import org.iocframework.staffFactory

class Spec extends FlatSpec with Matchers {
  "XML worker defs" should "output using an XML writer" in {
    val staffing = Staffing()
    staffing.addRecruiter("scala.xml", "xml", postJobXml)
    //staffing.addRecruiter("scala.xml", "dtd", postJobDtd)
    staffing.addRecruiter("scala.xml.element", null, postJobElement)
    val conf = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(`#html`))
"""
    val xformed = staffing.transformIoC(s"{$conf}".parse[Stat].get)
 
    val (factory, _) = staffFactory(conf, staffing = staffing)
//    val xformed = staffing.transformIoC(
//        q"""
//`namespace x|scala.xml`
//`namespace|scala.xml.element`
//
//`#x|xml`(encoding = "utf-8", version = "1.0"
//, dtd = "<!DOCTYPE html>"
//, `#html`(xmlns="http://www.w3.org/1999/xhtml", lang="en-us"
//  , "insert text here", "comments can be inseted, too"
//  )
//)
//"""
//    )
//    println(xformed)
    xformed.syntax shouldBe q"""{
  factory.setManager("xml", (c: Map[Any, Any]) => {
    val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))
    typeclass.writeStartDocument(typeclass.cast(writer))
    ((c: Map[Any, Any]) => {
      ((c: Map[Any, Any]) => if (c("cdata") == ()) () else {
        val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))
        typeclass.writeCharacters(typeclass.cast(writer), c("cdata").toString)
      })(c + ("cdata" -> {
        val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))
        typeclass.writeStartElement(typeclass.cast(writer), "html")
        ((c: Map[Any, Any]) => {})(c)
        typeclass.writeEndElement(typeclass.cast(writer))
      }))
    })(c)
    typeclass.writeEndDocument(typeclass.cast(writer))
  })
}""".syntax
    val stringWriter = new java.io.StringWriter()
    factory.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter), XmlStreamWriter)))
    stringWriter.toString shouldBe "<?xml version=\"1.0\" ?><html></html>"
  }
}
