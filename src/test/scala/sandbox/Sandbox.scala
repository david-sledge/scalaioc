package sandbox

import org.scalatest._
import scala.tools.reflect.ToolBox

class Sandbox extends FlatSpec with Matchers {
  "The sandbox" should "just be an area for me to play around" in {
    import scala.reflect.runtime.universe._

//    println(showRaw(q"hmm.println[T]()"))
//    println(showRaw(q"println[T]()"))
//    println(showRaw(q"hmm.println[T]"))
//    println(showRaw(q""""TEST""""))

  }

}
