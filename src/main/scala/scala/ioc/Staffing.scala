package scala.ioc

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Seq
import scala.meta._

final class Staffing {

  def postManagerJob(name: Term.Name)
      (namespaceName: String, localName: String)
      (expr: Term, args: Seq[Term.Arg]): Tree = {
    val (named, _, leftovers) = mapArgs(Seq("worker"), args)
    q"""
factory.$name($expr, ${toWorker(named("worker"))})
"""
  }

  def postRefJob(name: Term.Name)
      (namespaceName: String, localName: String)
      (expr: Term, args: Seq[Term.Arg]): Tree = {
    val (named, _, leftovers) = mapArgs(Seq("id"), args)
    q"""
factory.$name(${named("id")}, c)
"""
  }

  private val recruiters = {
    TrieMap[String, TrieMap[String,
      (String, String) => (Term, Seq[Term.Arg]) => Tree]](
        Staffing.ScalaIocNamespaceName -> TrieMap[String,
          (String, String) => (Term, Seq[Term.Arg]) => Tree](
            "=" -> postManagerJob(q"setLazyManager"),
            "=>" -> postManagerJob(q"setManager"),
            "ref" -> postRefJob(q"putToWork"),
            "ref!" -> postRefJob(q"crackTheWhip"),
            "let" -> ((namespaceName, localName) => (expr, args) =>
                (args :\ null.asInstanceOf[Term])((block, acc) =>
                  if (acc == null) block.asInstanceOf[Term]
                  else q"""${toWorker(acc)}(c + (${block.asInstanceOf[Term]}))"""
                )
              ),
            "resource" -> ((namespaceName, localName) => (expr, args) => {
              val (named, _, leftovers) = mapArgs(Seq("path"), args)
              q"""org.iocframework.staffFactoryFromResource(${named("path")},
factory = factory, staffing = staffing)"""
            }),
            "def" -> ((namespaceName, localName) => (expr, args) => {
              val (named, _, leftovers) =
                mapArgs(Seq("localName", "defn"), args)
              org.iocframework.staffFactory(
                  q"""staffing.addRecruiter(${
                    if (expr == null) Lit.Null(null) else expr
                  }, ${
                    if (named contains "localName") named("localName")
                    else Lit.Null(null)
                  },
${named("defn")})""".syntax, null, this)
              q"()"
            }),
            "include" -> ((namespaceName, localName) => (expr, args) => {
              val (named, _, leftovers) = mapArgs(Seq("path", "encoding"), args)
              if (named contains "path")
                named("path") match {
                  case Lit.String(path) => {
                    val encoding =
                      if (named contains "encoding")
                        named("encoding") match {
                          case Lit.String(encoding) => encoding
                          case _ => throw new IllegalArgumentException(
                            "The optional 'encoding' argument if supplied must be a string literal")
                        }
                      else "utf-8"
                    scala.io.Source.fromFile(path, encoding).mkString.parse[Stat].get
                  }
                  case _ => throw new IllegalArgumentException(
                      "'path' argument must be a string literal")
                }
              else
                throw new IllegalArgumentException("'path' argument must be provided as a string literal")
              q"()"
            })
          ))
  }

  def transformIoC(defn: Tree): Tree = {

    val referrers = scala.collection.mutable.Map[String, String]()

    def handlePrefix(name: String, expr: Term, args: Seq[Term.Arg], els: Term) =
    {
      if (name.startsWith("#")) {
        val (namespaceName, localName) = {
          val referrerEnd = name.indexOf('#', 1)
      
          if (referrerEnd == -1) {
            val namespaceNameEnd = name.indexOf('|')
      
            if (namespaceNameEnd == -1)
              // default namespace
              (if (referrers contains null) referrers(null) else null,
                  name.substring(1))
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
          case Some(recruiter) => recruiter(expr, args)
          case None => throw new NoSuchRecruiterException(namespaceName, localName)
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
      case t @ Term.Apply(Term.Name(name), args) =>
        handlePrefix(name, null, args, t)
      case t @ Term.Apply(Term.Select(expr, Term.Name(name)), args) =>
        handlePrefix(name, expr, args, t)
      case t @ Term.ApplyInfix(expr, Term.Name(name), Nil, args) =>
        handlePrefix(name, expr, args, t)
      case t @ Term.Select(expr, Term.Name(name)) =>
        handlePrefix(name, expr, List(), t)
      case t @ Term.Block(stats) => {
        Term.Block(for {
          stat <- stats
          if (!extractReferrer(stat))
        } yield stat)
      }
      case t => if (extractReferrer(t)) q"()" else t match {
        case t @ Term.Name(name) => handlePrefix(name, null, List(), t)
        case t => t
      }
    }
  }

  def addRecruiter(namespaceName: String, localName: String,
      recruiter: (String, String) => (Term, Seq[Term.Arg]) => Tree) = {
    if (recruiter == null)
      throw new IllegalArgumentException("A recruiter may not be null")
    else
      if (recruiters contains namespaceName)
      {
        val recruitersInNamespace = recruiters(namespaceName)
        if (recruitersInNamespace contains localName)
          throw new RecruiterDefinedException(namespaceName, localName)
        else
          recruitersInNamespace += localName -> recruiter
      }
      else recruiters += namespaceName -> TrieMap(localName -> recruiter)

    ()
  }

  def getRecruiter(namespaceName: String, localName: String)
      : Option[(Term, Seq[Term.Arg]) => Tree] =
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
