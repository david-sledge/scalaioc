package scala.blm

import org.scalatest._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

class PreprocessorSpec extends FlatSpec with Matchers {
  "The preprocessor" should "allow macros to be added" in {
    val f = (nsOpt: Option[String], localName: String) => (objTreeOpt: Option[Tree], argTrees: Seq[Tree]) => Right(q"List(..$argTrees)")
    val preprocessor = Preprocessor()
    preprocessor.addMacro(Some("test"), Some("test"), f)
  }

  "The preprocessor" should "manipulate the scala AST" in {
    val f = (nsOpt: Option[String], localName: String) => (objTreeOpt: Option[Tree], argTrees: Seq[Tree]) => Right(q"List(..$argTrees)")
    val preprocessor = Preprocessor()
    preprocessor.addMacro(Some("test"), Some("test"), f)
    val code = "{`namespace test|test`;`#test#test`(1, 2, 3);`#test|test`()}"
    // obtain toolbox
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
    // generate the AST
    val tree = tb.parse(code)
    // manipulate tree
    val transformedTree = preprocessor.transformMacros(tree)
    println(transformedTree)
    println(q"""val str = "str";val num = 5;val flarg = (str, num);val (cheese, veg) = flarg;println(cheese);println(veg)""")
  }
}
