package scala.ioc

import scala.collection.concurrent.TrieMap
import scala.meta._

final class Staffing {
  private def postManagerJob(name: Term.Name)(args: Seq[Seq[Term.Arg]]): Tree = {
    val (named, _, leftovers) = makeArgs(Seq("id", "worker"), args)
    q"""
factory.$name(${named("id")}, (c: scala.collection.Map[Any, Any]) => ${named("worker").asInstanceOf[Term]})
"""
  }

  private def postRefJob(name: Term.Name)(args: Seq[Seq[Term.Arg]]): Tree = {
    val (named, _, leftovers) = makeArgs(Seq("id"), args)
    q"""
factory.$name(${named("id")}, c)
"""
  }

  private val recruiters = TrieMap[String, Seq[Seq[Term.Arg]] => Tree](
      "singleton" -> postManagerJob(q"setCacheManager"),
      "prototype" -> postManagerJob(q"setManager"),
      "ref" -> postRefJob(q"getCachedResult"),
      "reloadRef" -> postRefJob(q"putToWork"),
      "let" -> (args => {
        val (named, _, leftovers) = makeArgs(Seq("id", "value", "block"), args)
        q"""((c: scala.collection.Map[Any, Any]) => ${named("block").asInstanceOf[Term]})
(c + (${named("id").asInstanceOf[Term]} -> ${named("value").asInstanceOf[Term]}))"""
      }),
      "resource" -> (args => {
        val (named, _, leftovers) = makeArgs(Seq("fileName"), args)
        q"org.iocframework.staffFactoryFromFile(${named("fileName")}, factory, staffing)"
      }),
      "recruiterDef" -> (args => {
        val (named, _, leftovers) = makeArgs(Seq("name", "defn"), args)
        org.iocframework.staffFactory(
            q"staffing.addRecruiter(${named("name")}, ${named("defn")})".syntax, null, this)
        q"()"
      })
    )

  // TODO:  create unit test
  def transformIoC(defn: Tree) = {
    defn.transform {
      case t @ q"$expr(...$args)" =>
        expr match {
          case Term.Name(name) =>
            if (recruiters contains name) recruiters(name)(args) else t
          case _ => t
        }
      case t => t
    }
  }

  // TODO:  create unit test
  // TODO:  prevent core recruiters from being removed or replaced
  def addRecruiter(name: String, defn: Seq[Seq[Term.Arg]] => Tree) =
    recruiters += name -> defn

  // TODO:  create unit test
  def getRecruiter(name: String) = recruiters(name)

  // TODO:  create unit test
  def hasRecruiter(name: String) = recruiters contains name
}
