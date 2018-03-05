package scala.ioc

import scala.collection.concurrent.TrieMap
import scala.meta._

final class Staffing {
  private def postManagerJob(name: Term.Name)(args: Seq[Seq[Term.Arg]]): Tree = {
    val (named, _, leftovers) = mapArgs(Seq("id", "worker"), args)
    q"""
factory.$name(${named("id")}, ${toWorker(named("worker"))})
"""
  }

  private def postRefJob(name: Term.Name)(args: Seq[Seq[Term.Arg]]): Tree = {
    val (named, _, leftovers) = mapArgs(Seq("id"), args)
    q"""
factory.$name(${named("id")}, c)
"""
  }

  private val recruiters = TrieMap[String, Seq[Seq[Term.Arg]] => Tree](
      //"test" -> (args => q"""let("id", "value", "block")"""),
      "singleton" -> postManagerJob(q"setLazyManager"),
      "prototype" -> postManagerJob(q"setManager"),
      "ref" -> postRefJob(q"getCachedResult"),
      "reloadRef" -> postRefJob(q"putToWork"),
      "let" -> (args => {
        val (named, _, leftovers) = mapArgs(Seq("id", "value", "block"), args)
        q"""${toWorker(named("block"))}
(c + (${named("id")} -> ${named("value")}))"""
      }),
      "resource" -> (args => {
        val (named, _, leftovers) = mapArgs(Seq("fileName"), args)
        q"org.iocframework.staffFactoryFromFile(${named("fileName")}, factory, staffing)"
      }),
      "recruiterDef" -> (args => {
        val (named, _, leftovers) = mapArgs(Seq("name", "defn"), args)
        org.iocframework.staffFactory(
            q"staffing.addRecruiter(${named("name")}, ${named("defn")})".syntax, null, this)
        q"()"
      })
    )

  // TODO:  create unit test
  def transformIoC(defn: Tree): Tree = {
    defn.transform {
      case t @ q"$expr(...$args)" => {
        //println("*******************************************************************************")
        //println(t.syntax)
        expr match {
          case Term.Name(name) => {
            if (recruiters contains name) transformIoC(recruiters(name)(args)) else t
          }
          case _ => t
        }
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

object Staffing {
  def apply() = new Staffing
}