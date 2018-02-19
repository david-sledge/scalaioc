package scala

import meta._
//import scala.collection.immutable.{Seq, List}

package object ioc {
  /**
   * merely a place-holder.  Calls to this method are replaced via macro
   */
  def prototype(id: Any, worker: Any) = {
    println("prototype")
  }

  /**
   * merely a place-holder.  Calls to this method are replaced via macro
   */
  def singleton(id: Any, worker: Any) = {
    println("singleton")
  }

  def ref(id: Any): Any = {
    println("ref")
  }

  def reloadRef(id: Any): Any = {
    println("reloadRef")
  }

  def let(name: Any, value: Any, block: Any): Any = {
    println("let")
  }

  def resource(fileName: String): Any = {
    println("resource")
  }

  // TODO:  create unit test
  def makeArgs(seqArgNames: Seq[String], args: Seq[Seq[Term.Arg]]) = {
    // address the named arguments first, then handle the ordinal arguments
    val (ordNames, named, ordinal) = args.flatten.foldRight((seqArgNames, Map[String, Term.Arg](), List[Term.Arg]()))(
        (t: Term.Arg, acc: (Seq[String], Map[String, Term.Arg], List[Term.Arg])) =>
          acc match {
            case (ordArgNames, map, list) =>
              t match {
                case Term.Arg.Named(Term.Name(name), expr) => (
                    {
                      val ndx = ordArgNames indexOf name
                      if (ndx == -1) ordArgNames else ordArgNames.take(ndx) ++ ordArgNames.drop(ndx + 1)
                    }
                    , map + (name -> expr), list)
                case _ => (ordArgNames, map, t :: list)
              }
            }
        )

    // fill the gaps with the ordinal arguments
    ordinal.foldLeft(named, ordNames)(
        (acc: (Map[String, Term.Arg], Seq[String]), t: Term.Arg) =>
          acc match {
            case (map, ordArgNames) => {
              ordArgNames.size match {
                case 0 => map -> ordArgNames
                case _ => map + (ordArgNames(0) -> t) -> ordArgNames.drop(1)
              }
            }
          }
        )
  }

  private def postManagerJob(name: Term.Name)(args: Seq[Seq[Term.Arg]]): Tree = {
    val (named, leftovers) = makeArgs(Seq("id", "worker"), args)
    q"""
factory.$name(${named("id")}, (c: scala.collection.Map[Any, Any]) => ${named("worker").asInstanceOf[Term]})
"""
  }

  private def postRefJob(name: Term.Name)(args: Seq[Seq[Term.Arg]]): Tree = {
    val (named, leftovers) = makeArgs(Seq("id"), args)
    q"""
factory.$name(${named("id")}, c)
"""
  }
}