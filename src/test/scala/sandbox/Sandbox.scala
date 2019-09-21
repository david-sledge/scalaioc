package sandbox

import org.scalatest._
import scala.tools.reflect.ToolBox

class Sandbox extends FlatSpec with Matchers {
  "This" should "just be an area for me to play around" in {
    import scala.reflect.runtime.universe._

//    println(showRaw(q"new A(b, c = c)"))
//
//    q"String" match {
//      case Ident(TermName(t)) => println(showRaw(q"variable.asInstanceOf[${Ident(TypeName(t))}]"))
//    }

//    println(showRaw(q"a.asInstanceOf[(Option[String], List[Int])]"))
//    println(showRaw(q"A[B, E](c, d)"))
//    println(showRaw(q"(B, E)"))
//    val tb = scala.reflect.runtime.universe.runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
//    val code = s"A[${q"(B, E)".toString}]"
//    println(code)
//    println(showRaw(tb.parse(code)))
//    val list = List[Any](0)
//    val intList = Array[Int](1)
//    println(intList(scala.ioc.cast(list.head)))
//    ((s: String) => 0).asInstanceOf[String => Int]
    val argList = List(q"()", AssignOrNamedArg(Ident(TermName("x")), q"5"))
    println(showRaw(q"test($argList)"))
  }
}

/*
Apply(
  TypeApply(Ident(TermName("A")), List(Ident(TypeName("B")))),
  List(Ident(TermName("c")), Ident(TermName("d")))
)
* */
