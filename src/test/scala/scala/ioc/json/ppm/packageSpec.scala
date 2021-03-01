package scala.ioc.json.ppm

import org.scalatest._
import scala.ioc.ppm._
import scala.ppm._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

class packageSpec extends FlatSpec with Matchers {

  val preprocessor = {
    val preprocessor = populateStaffingMacros()
    preprocessor.addMacro(Some("scalaioc.json"), Some("{}"), postJobObject)
    preprocessor.addMacro(Some("scalaioc.json"), Some("[]"), postJobArray)
    preprocessor.addMacro(Some("scalaioc.json"), Some("json"), postJobJson)
    preprocessor
  }

  "JSON worker defs" should "output using a JSON writer" in {

    def test(conf: String, expected: String) = {
//      println(preprocessor.transformTree(tb.parse(conf), tb))
      val (factory, _) = staffFactory(conf, preprocessor = preprocessor)
      val stringWriter = new java.io.StringWriter()
      val jGen = new com.fasterxml.jackson.core.JsonFactory().createGenerator(stringWriter)
      factory.putToWork("json", Map("jsonWriter" -> jGen))
      jGen.flush()
      stringWriter.toString shouldBe expected
    }

    test("""
`namespace|scalaioc.json`
`namespace i|scalaioc`

"json" `#i|=>` `#json`(2)
""", "2")

    test("""
`namespace|scalaioc.json`
`namespace i|scalaioc`

"json" `#i|=>` `#json`(2.2)
""", "2.2")

    test("""
`namespace|scalaioc.json`
`namespace i|scalaioc`

"json" `#i|=>` `#json`(true)
""", "true")

    test("""
`namespace|scalaioc.json`
`namespace i|scalaioc`

"json" `#i|=>` `#json`("2 None false 2.2")
""", "\"2 None false 2.2\"")

    test("""
`namespace|scalaioc.json`
`namespace i|scalaioc`

"json" `#i|=>` `#json`(None)
""", "null")

    test("""
`namespace|scalaioc.json`
`namespace i|scalaioc`

"json" `#i|=>` `#[]`
""", "[]")

    test("""
`namespace|scalaioc.json`
`namespace i|scalaioc`

"json" `#i|=>` `#{}`
""", "{}")

    test("""
`namespace|scalaioc.json`
`namespace i|scalaioc`

"json" `#i|=>` `#[]`(2, 2.2, true, "2 None false 2.2", None, `#{}`)
""", "[2,2.2,true,\"2 None false 2.2\",null,{}]")

    test("""
`namespace|scalaioc.json`
`namespace i|scalaioc`

"json" `#i|=>` `#{}`(
  `int` = 2,
  `2.2` = 2.2,
  `true` = true,
  `string` = "2 None false 2.2",
  bottom = None,
  array = `#[]`(),
)
""", "{\"int\":2,\"2.2\":2.2,\"true\":true,\"string\":\"2 None false 2.2\",\"bottom\":null,\"array\":[]}")

  }

}
