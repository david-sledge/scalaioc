package scala.ioc.xml.ppm

import org.scalatest._
import scala.ioc.ppm._
import scala.ioc.xml._
import scala.ppm._
import scala.xml._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

class packageSpec extends FlatSpec with Matchers {

  val preprocessor = {
    val preprocessor = populateStaffingMacros()
    preprocessor.addMacro(Some("scalaioc.xml"), Some("xml"), postJobXml)
    preprocessor.addMacro(Some("scalaioc.xml"), Some("cdata"), postJobCdata)
    preprocessor.addMacro(Some("scalaioc.xml"), Some("!"), postJobComment)
    preprocessor.addMacro(Some("scalaioc.xml"), Some("?"), postJobProcInstr)
    preprocessor.addMacro(Some("scalaioc.xml.element"), None, postJobElement)
    preprocessor
  }

  val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")

//  "XML worker defs" should "expand to AST that streams out XML" in {
//
//    println(preprocessor.transformTree(q"""
//`#scalaioc.xml#xml`(
////  enc = "utf-8",
//  ver = "1.1",
//  dtd = "<!DOCTYPE NEWSPAPER [<!ENTITY COPYRIGHT \"Copyright 1998 Vervet Logic Press\">]>",
//  `#scalaioc.xml.element#html`(xmlns = "http://www.w3.org/1999/xhtml"),
//)
//""", tb, None))
//
//  }

  "XML worker defs" should "output using an XML writer" in {

    def test(conf: String, expected: String) = {
//      println(preprocessor.transformTree(tb.parse(conf), tb))
      val (factory, _) = staffFactory(conf, preprocessor = preprocessor)
      val stringWriter = new java.io.StringWriter()
      factory.putToWork("xml", Map("xmlWriter" -> javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(stringWriter)))
      stringWriter.toString shouldBe expected
    }

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#html`
""", "<html></html>")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(`#html`)
""", """<?xml version="1.0" ?><html></html>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(ver = "1.0", `#html`(xmlns = "http://www.w3.org/1999/xhtml"))
""", """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"></html>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(enc = "utf-8", `#html`(xmlns = "http://www.w3.org/1999/xhtml"))
""", """<?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml"></html>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(enc = "utf-8", `#html`(xmlns = "http://www.w3.org/1999/xhtml"), ver = "1.0")
""", """<?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml"></html>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml"), ver = "1.0", dtd="<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(dtd = "<!DOCTYPE html>", `#html`(xmlns = "http://www.w3.org/1999/xhtml"), ver = "1.0")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(
  `#html`(xmlns = "http://www.w3.org/1999/xhtml"),
  ver = "1.0",
  dtd="<!DOCTYPE html>",
  `#x|!`("end of document"),
)
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html><!--end of document-->""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml"), ver = "1.0", dtd = "<!DOCTYPE html>", `#x|cdata`("end of document"))
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"></html><![CDATA[end of document]]>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml", `#x|?`("scala")), ver = "1.0", dtd = "<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><?scala?></html>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml", `#x|?`("scala", "cheese")), ver = "1.0", dtd="<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><?scala cheese?></html>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(`#html`(xmlns = "http://www.w3.org/1999/xhtml", "textual data"), ver = "1.0", dtd="<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml">textual data</html>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(`#html`(`#head`, xmlns = "http://www.w3.org/1999/xhtml", "textual data"), ver = "1.0", dtd="<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><head></head>textual data</html>""")

    test("""
`namespace x|scalaioc.xml`
`namespace|scalaioc.xml.element`
`namespace i|scalaioc`

"xml" `#i|=>` `#x|xml`(`#html`(`#head`, xmlns = "http://www.w3.org/1999/xhtml", `#body`("<script></script>")), ver = "1.0", dtd="<!DOCTYPE html>")
""", """<?xml version="1.0"?><!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><head></head><body>&lt;script&gt;&lt;/script&gt;</body></html>""")
  }

}
