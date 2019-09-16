package sandbox

import org.scalatest._

class Sandbox extends FlatSpec with Matchers {
  "This" should "just be an area for me to play around" in {
    import scala.reflect.runtime.universe._

    println(showRaw(q"new A(b, c = c)"))

    q"String" match {
      case Ident(TermName(t)) => println(showRaw(q"variable.asInstanceOf[${Ident(TypeName(t))}]"))
    }
  }
}
