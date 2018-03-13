package scala.ioc

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Seq
import scala.meta._

final class Staffing {

  private def postManagerJob(name: Term.Name)
      (namespaceName: String, localName: String)
      (args: Seq[Term.Arg]): Tree = {
    val (named, _, leftovers) = mapArgs(Seq("id", "worker"), args)
    q"""
factory.$name(${named("id")}, ${toWorker(named("worker"))})
"""
  }

  private def postRefJob(name: Term.Name)
      (namespaceName: String, localName: String)
      (args: Seq[Term.Arg]): Tree = {
    val (named, _, leftovers) = mapArgs(Seq("id"), args)
    q"""
factory.$name(${named("id")}, c)
"""
  }

  private val recruiters = {
    TrieMap[String, TrieMap[String, (String, String) => Seq[Term.Arg] => Tree]](
        Staffing.ScalaIocNamespaceName -> TrieMap[String,
          (String, String) => Seq[Term.Arg] => Tree](
            "=" -> postManagerJob(q"setLazyManager"),
            "=>" -> postManagerJob(q"setManager"),
            "ref" -> postRefJob(q"putToWork"),
            "reloadRef" -> postRefJob(q"crackTheWhip"),
            "let" -> ((namespaceName, localName) => args => {
              val (named, _, leftovers) =
                mapArgs(Seq("id", "value", "block"), args)
              q"""${toWorker(named("block"))}(
c + (${named("id")} -> ${named("value")}))"""
            }),
            "resource" -> ((namespaceName, localName) => args => {
              val (named, _, leftovers) = mapArgs(Seq("fileName"), args)
              q"""org.iocframework.staffFactoryFromFile(${named("fileName")},
factory, staffing)"""
            }),
            "recruiterDef" -> ((namespaceName, localName) => args => {
              val (named, _, leftovers) = mapArgs(Seq("name", "defn"), args)
              org.iocframework.staffFactory(
                  q"""staffing.addRecruiter(${named("name")},
${named("defn")})""".syntax, null, this)
              q"()"
            })
          ))
  }

  def transformIoC(defn: Tree): Tree = {

    val referrers = scala.collection.mutable.Map[String, String]()

    def handlePrefix(name: String, args: Seq[Term.Arg], els: Term) = {
      if (name.startsWith("#")) {
        val (namespaceName, localName) = {
          val referrerEnd = name.indexOf('#', 1)
      
          if (referrerEnd == -1) {
            val namespaceNameEnd = name.indexOf('|')
      
            if (namespaceNameEnd == -1)
              // default namespace
              (if (referrers contains null) referrers(null) else null, name.substring(1))
            else if (namespaceNameEnd == 1)
              // no namespace
              (null, name.substring(namespaceNameEnd + 1))
            else
              // referred namespace
              (referrers(name.substring(1, namespaceNameEnd)),
                  name.substring(namespaceNameEnd + 1))
          }
          else
            // explicitly named namespace
            (name.substring(1, referrerEnd), name.substring(referrerEnd + 1))
        }
        getRecruiter(namespaceName, localName) match {
          case Some(recruiter) => recruiter(args)
          case None => Term.Name("xformedName") // TODO: throwException
        }
      }
      else els
    }

    def extractReferrer(stat: Tree) = {
      stat match {
        case Term.Name(name) => {
          if (name.startsWith("namespace ")) {
            val pos = name.indexOf('|')
      
            if (pos == -1) {
              false
            } else {
              referrers += name.substring(10, pos) -> name.substring(pos + 1)
              true
            }
          }
          else if (name.startsWith("namespace|")) {
            referrers += ((null, name.substring(10)))
            true
          }
          else {
            false
          }
        }
        case _ => false
      }
    }

    defn.transform {
      case t @ Term.Apply(Term.Name(name), args) => handlePrefix(name, args, t)
      case t @ Term.ApplyInfix(expr, Term.Name(name), Nil, args) => handlePrefix(name, expr +: args, t)
      case t @ Term.Block(stats) => {
        Term.Block(for {
          stat <- stats
          if (!extractReferrer(stat))
        } yield stat)
      }
      case t => if (extractReferrer(t)) q"()" else t match {
        case t @ Term.Name(name) => handlePrefix(name, List(), t)
        case t => t
      }
    }
  }

  // TODO:  possibly prevent any recruiters from being removed or replaced
  def addRecruiter(namespaceName: String, localName: String,
      recruiter: (String, String) => Seq[Term.Arg] => Tree) =
    if (namespaceName == Staffing.ScalaIocNamespaceName) () // TODO:  throw exception
    else
      if (recruiters contains namespaceName)
        recruiters(namespaceName) += localName -> recruiter
      else recruiters += namespaceName -> TrieMap(localName -> recruiter)

  def getRecruiter(namespaceName: String, localName: String)
      : Option[Seq[Term.Arg] => Tree] =
    recruiters.get(namespaceName) match {
      case Some(recruitersInNamespace) =>
          recruitersInNamespace.get(localName) match {
            case Some(recruiter) => Some(recruiter(namespaceName, localName))
            case None => recruitersInNamespace.get(null) match {
              case Some(recruiter) => Some(recruiter(namespaceName, localName))
              case None => None
            }
          }
      case None => None
    }

  private def hasRecruitersInNamespace(namespaceName: String) =
    (recruiters contains namespaceName)

  def hasRecruiter(namespaceName: String, localName: String) =
    hasRecruitersInNamespace(namespaceName: String) &&
    (
        (recruiters(namespaceName) contains localName) ||
        (recruiters(namespaceName) contains null)
    )

  def hasOwnRecruiter(namespaceName: String, localName: String) =
    hasRecruitersInNamespace(namespaceName: String) &&
    (recruiters(namespaceName) contains localName)
}

object Staffing {
  val ScalaIocNamespaceName = "scala.ioc"

  def apply() = new Staffing
}
