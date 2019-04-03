package sandbox

import org.scalatest._
import scala.ioc._
import scala.reflect.runtime.universe._

class Sandbox extends FlatSpec with Matchers {
  "This" should "just be an area for me to play around" in {
    import scala.reflect.runtime.universe._
    import scala.tools.reflect.ToolBox

    // obtain toolbox
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox()
    // we'll say this code came from "MyCode"
    val tree1 = tb.parse("println(42)")
    // this one came from "YourCode"
    val tree2 = tb.parse("""println("is the answer")""")
    // and this one came from "TheirCode"
    val tree3 = tb.parse("""println("to life, the universe, and everything")""")
    // an over-simplified combination of the trees
    val frankensteinsTree = q"$tree1;$tree2;$tree3;println(test)"
    //val tree = atPos(tree3.pos)(q"$tree1;$tree2;$tree3;$frankensteinsTree")

    // walk the tree an print the source of each element
    val traverser = new Traverser() {

      override def traverse(tree: Tree): Unit = {

        println("This node originated from " + tree.pos.source)
        super.traverse(tree)
      }
    }

    // the root element prints "This node originated from <no source file>"
    // the rest print "This node originated from <toolbox>"
    traverser.traverse(frankensteinsTree)
  }
}
