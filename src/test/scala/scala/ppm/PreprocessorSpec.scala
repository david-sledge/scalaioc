package scala.ppm

import org.scalatest._
import scala.reflect.runtime.universe
  import universe._
import scala.tools.reflect.ToolBox

class PreprocessorSpec extends FlatSpec with Matchers {
  "The preprocessor" should "allow macros to be added" in {
    val f = (nsOpt: Option[String], localName: String) => (objTreeOpt: Option[Tree], argTrees: Seq[Tree], tb: ToolBox[universe.type], src: Option[String]) => q"List(..$argTrees)"
    val preprocessor = Preprocessor()
    preprocessor.addMacro(Some("test"), Some("test"), f)
  }

  it should "manipulate the scala AST" in {
    val f = (nsOpt: Option[String], localName: String) => (objTreeOpt: Option[Tree], argTrees: Seq[Tree], tb: ToolBox[universe.type], src: Option[String]) => q"List(..$argTrees)"
    val preprocessor = Preprocessor()
    preprocessor.addMacro(Some("test"), Some("test"), f)
    val code = "{`namespace test|test`;`#test#test`(1, 2, 3);`#test|test`()}"
    // obtain toolbox
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
    // generate the AST
    val tree = tb.parse(code)
    // manipulate tree
    val transformedTree = preprocessor.transformTree(tree, tb, Some("PreprocessorSpec"))
    transformedTree.toString shouldBe q"""{();List(1, 2, 3);List()}""".toString
  }
}
