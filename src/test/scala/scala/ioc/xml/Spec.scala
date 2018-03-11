package scala.ioc

import org.scalatest._
import scala.ioc.xml._
import scala.xml.stream._
import scala.meta._
import org.iocframework.staffFactory

class Spec extends FlatSpec with Matchers {
  val staffing = {
    val staffing = new Staffing()
    staffing.addRecruiter("scala.xml", "xml", postJobXml)
    staffing.addRecruiter("scala.xml", "dtd", postJobDtd)
    staffing.addRecruiter("scala.xml", "cdata", postJobCdata)
    staffing.addRecruiter("scala.xml", "!", postJobComment)
    staffing.addRecruiter("scala.xml", "?", postJobProcInstr)
    staffing.addRecruiter("scala.xml.element", null, postJobElement)
    staffing
  }

  "XML worker defs" should "output using an XML writer" in {
    val conf0 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(`#html`))
"""
    val (factory0, _) = staffFactory(conf0, staffing = staffing)
    val stringWriter0 = new java.io.StringWriter()
    factory0.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter0), XmlStreamWriter)))
    stringWriter0.toString shouldBe "<?xml version=\"1.0\" ?><html></html>"

    val conf1 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(version = "1.0", `#html`(xmlns = "http://www.w3.org/1999/xhtml")))
"""
    val (factory1, _) = staffFactory(conf1, staffing = staffing)
    val stringWriter1 = new java.io.StringWriter()
    factory1.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter1), XmlStreamWriter)))
    stringWriter1.toString shouldBe """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"></html>"""

    val conf2 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(encoding = "utf-8", `#html`(xmlns = "http://www.w3.org/1999/xhtml")))
"""
    val (factory2, _) = staffFactory(conf2, staffing = staffing)
    val stringWriter2 = new java.io.StringWriter()
    factory2.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter2), XmlStreamWriter)))
    stringWriter2.toString shouldBe """<?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml"></html>"""

    val conf3 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(encoding = "utf-8", `#html`(xmlns = "http://www.w3.org/1999/xhtml"), version = "1.0"))
"""
    val (factory3, _) = staffFactory(conf3, staffing = staffing)
    val stringWriter3 = new java.io.StringWriter()
    factory3.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter3), XmlStreamWriter)))
    stringWriter3.toString shouldBe """<?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml"></html>"""

    val conf4 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml"), version = "1.0", dtd="<!DOCTYPE html>"))
"""
    val (factory4, _) = staffFactory(conf4, staffing = staffing)
    val stringWriter4 = new java.io.StringWriter()
    factory4.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter4), XmlStreamWriter)))
    stringWriter4.toString shouldBe """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html>"""

    val conf5 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(`#x|dtd`("<!DOCTYPE html>"), `#html`(xmlns = "http://www.w3.org/1999/xhtml"), version = "1.0"))
"""
    val (factory5, _) = staffFactory(conf5, staffing = staffing)
    val stringWriter5 = new java.io.StringWriter()
    factory5.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter5), XmlStreamWriter)))
    stringWriter5.toString shouldBe """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html>"""

    val conf6 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml"), version = "1.0", dtd="<!DOCTYPE html>", `#x|!`("end of document")))
"""
    val (factory6, _) = staffFactory(conf6, staffing = staffing)
    val stringWriter6 = new java.io.StringWriter()
    factory6.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter6), XmlStreamWriter)))
    stringWriter6.toString shouldBe """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html><!--end of document-->"""

    val conf7 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml"), version = "1.0", dtd="<!DOCTYPE html>", `#x|cdata`("end of document")))
"""
    val (factory7, _) = staffFactory(conf7, staffing = staffing)
    val stringWriter7 = new java.io.StringWriter()
    factory7.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter7), XmlStreamWriter)))
    stringWriter7.toString shouldBe """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html><![CDATA[end of document]]>"""

    val conf8 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml", `#x|?`("scala")), version = "1.0", dtd="<!DOCTYPE html>"))
"""
    val (factory8, _) = staffFactory(conf8, staffing = staffing)
    val stringWriter8 = new java.io.StringWriter()
    factory8.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter8), XmlStreamWriter)))
    stringWriter8.toString shouldBe """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><?scala?></html>"""

    val conf9 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml", `#x|?`("scala", "cheese")), version = "1.0", dtd="<!DOCTYPE html>"))
"""
    val (factory9, _) = staffFactory(conf9, staffing = staffing)
    val stringWriter9 = new java.io.StringWriter()
    factory9.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter9), XmlStreamWriter)))
    stringWriter9.toString shouldBe """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><?scala cheese?></html>"""

    val conf10 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml", "textual data"), version = "1.0", dtd="<!DOCTYPE html>"))
"""
    val (factory10, _) = staffFactory(conf10, staffing = staffing)
    val stringWriter10 = new java.io.StringWriter()
    factory10.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter10), XmlStreamWriter)))
    stringWriter10.toString shouldBe """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml">textual data</html>"""

    val conf11 = """
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

`#ioc|prototype`("xml", `#x|xml`(`#html`(`#head`, xmlns = "http://www.w3.org/1999/xhtml", "textual data"), version = "1.0", dtd="<!DOCTYPE html>"))
"""
    val (factory11, _) = staffFactory(conf11, staffing = staffing)
    val stringWriter11 = new java.io.StringWriter()
    factory11.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter11), XmlStreamWriter)))
    stringWriter11.toString shouldBe """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><head></head>textual data</html>"""
  }
}
