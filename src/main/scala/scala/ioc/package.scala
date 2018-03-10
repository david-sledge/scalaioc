package scala

import meta._
import scala.collection.immutable

package object ioc {

  def toWorker(stat: Term) = {
    q"""
(c: Map[Any, Any]) => $stat
"""
  }

  /**
   * Takes the given arguments and assigns them to the given argument names
   * using the following rules:
   * - named arguments are handled first
   * - ordinal arguments will be assigned to the variable names in the order
   *   they appear
   */
  def mapArgs(seqArgNames: immutable.Seq[String], args: immutable.Seq[Term.Arg]):
      (Map[String, Term], immutable.Seq[Term.Arg], immutable.Seq[String]) = {
    // address the named arguments first, then handle the ordinal arguments
    val (named, ordinal, ordNames) = args.foldRight(Map[String, Term](), List[Term.Arg](), seqArgNames)(
        (t, acc) =>
          acc match {
            case (map, list, ordArgNames) =>
              t match {
                case Term.Arg.Named(Term.Name(name), expr) => (
                    map + (name -> expr.asInstanceOf[Term]), list, {
                      val ndx = ordArgNames indexOf name
                      if (ndx == -1) ordArgNames else ordArgNames.take(ndx) ++ ordArgNames.drop(ndx + 1)
                    })
                case _ => (map, t :: list, ordArgNames)
              }
            }
        )

    // fill the gaps with the ordinal arguments
    val (named0, ordinal0, ordNames0) = ordinal.foldLeft(named, immutable.Seq[Term.Arg](), ordNames)(
        (acc, t) =>
          acc match {
            case (map, ord, ordArgNames) => {
              ordArgNames.size match {
                case 0 => (map, ord :+ t, ordArgNames)
                case _ => (map + (ordArgNames(0) -> t.asInstanceOf[Term]), ord, ordArgNames.drop(1))
              }
            }
          }
        )

    (named0, ordinal0.reverse, ordNames0)
  }
}