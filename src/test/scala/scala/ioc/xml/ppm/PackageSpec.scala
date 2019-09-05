package scala.ioc.xml.ppm

import org.scalatest._
import scala.ioc.ppm._
import scala.ioc.xml._
import scala.ppm._
import scala.xml._
import scala.reflect.runtime.universe._

class PackageSpec extends FlatSpec with Matchers {
  val preprocessor = {
    val preprocessor = new Preprocessor()
    preprocessor.addMacro(Some("scala.xml"), Some("xml"), postJobXml)
    preprocessor.addMacro(Some("scala.xml"), Some("dtd"), postJobDtd)
    preprocessor.addMacro(Some("scala.xml"), Some("cdata"), postJobCdata)
    preprocessor.addMacro(Some("scala.xml"), Some("!"), postJobComment)
    preprocessor.addMacro(Some("scala.xml"), Some("?"), postJobProcInstr)
    preprocessor.addMacro(Some("scala.xml.element"), None, postJobElement)
    preprocessor
  }

  "XML worker defs" should "output using an XML writer" in {

    def test(conf: String, expected: String) = {
      val (factory, _) = staffFactory(conf, preprocessor = preprocessor)
      val stringWriter = new java.io.StringWriter()
      factory.putToWork("xml", Map("xmlWriter" -> (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter), stream.write)))
      stringWriter.toString shouldBe expected
    }

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#html`
""", "<html></html>")

    test("""
`namespace x|scala.xml`
`namespace|scala.xml.element`
`namespace ioc|scala.ioc`

"xml" `#ioc|=>` `#x|xml`(`#html`)
""", """<?xml version="1.0" ?><html></html>""")

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
