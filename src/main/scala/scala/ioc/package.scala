package scala

import meta._

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

  /**
   * merely a place-holder.  Calls to this method are replaced via macro
   */
  def ref(id: Any): Any = {
    println("ref")
  }

  /**
   * merely a place-holder.  Calls to this method are replaced via macro
   */
  def reloadRef(id: Any): Any = {
    println("reloadRef")
  }

  /**
   * merely a place-holder.  Calls to this method are replaced via macro
   */
  def let(name: Any, value: Any, block: Any): Any = {
    println("let")
  }

  /**
   * merely a place-holder.  Calls to this method are replaced via macro
   */
  def resource(fileName: String): Any = {
    println("resource")
  }

  def flattenBlock(block: Term.Block): Seq[Stat] = block match {
    case Term.Block(s) => s.foldRight(Seq[Stat]())(
        (t, acc) => t match {
          case b @ Term.Block(_) => acc ++ flattenBlock(b)
          case _ => acc :+ t
        }
      )
  }

  def toWorker(stat: Term) = {
    q"""
(c: scala.collection.Map[Any, Any]) => $stat
"""
  }

  /**
   * Takes the given arguments and assigns them to the given argument names
   * using the following rules:
   * - named arguments are handled first
   */
  // TODO:  create unit test
  def mapArgs(seqArgNames: Seq[String], args: Seq[Seq[Term.Arg]]):
      (Map[String, Term], Seq[Term], Seq[String]) = {
    // address the named arguments first, then handle the ordinal arguments
    val (named, ordinal, ordNames) = args.flatten.foldRight(Map[String, Term](), List[Term](), seqArgNames)(
        (t, acc) =>
          acc match {
            case (map, list, ordArgNames) =>
              t match {
                case Term.Arg.Named(Term.Name(name), expr) => (
                    map + (name -> expr.asInstanceOf[Term]), list, {
                      val ndx = ordArgNames indexOf name
                      if (ndx == -1) ordArgNames else ordArgNames.take(ndx) ++ ordArgNames.drop(ndx + 1)
                    })
                case _ => (map, t.asInstanceOf[Term] :: list, ordArgNames)
              }
            }
        )

    // fill the gaps with the ordinal arguments
    ordinal.foldLeft(named, Seq[Term](), ordNames)(
        (acc, t) =>
          acc match {
            case (map, ord, ordArgNames) => {
              ordArgNames.size match {
                case 0 => (map, ord :+ t, ordArgNames)
                case _ => (map + (ordArgNames(0) -> t), ord, ordArgNames.drop(1))
              }
            }
          }
        )
  }
}