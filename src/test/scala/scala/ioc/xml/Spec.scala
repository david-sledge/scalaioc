package scala.ioc

import org.scalatest._
import scala.ioc.xml._
import scala.xml.writer._
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

    def test(conf: String, expected: String) = {
      val (factory, _) = staffFactory(conf, staffing = staffing)
      val stringWriter = new java.io.StringWriter()
      factory.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter), XmlStreamWriter.xmlStreamWriter)))
      stringWriter.toString shouldBe expected
    }

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(`#html`)
""", "<?xml version=\"1.0\" ?><html></html>")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(version = "1.0", `#html`(xmlns = "http://www.w3.org/1999/xhtml"))
""", """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"></html>""")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(encoding = "utf-8", `#html`(xmlns = "http://www.w3.org/1999/xhtml"))
""", """<?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml"></html>""")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(encoding = "utf-8", `#html`(xmlns = "http://www.w3.org/1999/xhtml"), version = "1.0")
""", """<?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml"></html>""")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml"), version = "1.0", dtd="<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html>""")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(`#x|dtd`("<!DOCTYPE html>"), `#html`(xmlns = "http://www.w3.org/1999/xhtml"), version = "1.0")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html>""")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml"), version = "1.0", dtd="<!DOCTYPE html>", `#x|!`("end of document"))
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html><!--end of document-->""")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml"), version = "1.0", dtd="<!DOCTYPE html>", `#x|cdata`("end of document"))
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html><![CDATA[end of document]]>""")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml", `#x|?`("scala")), version = "1.0", dtd="<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><?scala?></html>""")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml", `#x|?`("scala", "cheese")), version = "1.0", dtd="<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><?scala cheese?></html>""")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml", "textual data"), version = "1.0", dtd="<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml">textual data</html>""")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(`#html`(`#head`, xmlns = "http://www.w3.org/1999/xhtml", "textual data"), version = "1.0", dtd="<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><head></head>textual data</html>""")
  }
}
