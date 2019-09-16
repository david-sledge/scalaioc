package scala.ppm

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Map
import scala.reflect.runtime.universe._
import scala.reflect.NameTransformer._
import scala.annotation.tailrec

final class Preprocessor {

  private val macros = TrieMap[
    Option[String],
    TrieMap[
      Option[String],
      (Option[String], String) =>
        (Option[Tree], List[Tree]) => Tree
    ]
  ]()

  private val transformer = new Transformer() {

    var referrers = List[Map[Option[String], Option[String]]]()

    def getReferrer(ref: Option[String]) = {
      referrers match {
        case head +: tail => {
          head.get(ref)
        }
        case _ => None
      }
    }

    def setReferrer(ref: Option[String], namespaceName: Option[String]) = {
      referrers = referrers match {
        case head +: tail => {
          (head + (ref -> namespaceName)) +: tail
        }
        case _ => referrers
      }
    }

    override def transform(tree: Tree): Tree = {

      def parseQname(name: String, pos: Position) = {

        if (name.startsWith(Preprocessor.MacroStart)) {

          val namespaceEnd = name.indexOf(Preprocessor.MacroStart, Preprocessor.MacroStart.length())

          val (ns, localName) = if (namespaceEnd == -1) {
            val referrerEnd = name.indexOf(Preprocessor.ReferrerTerminator)

            if (referrerEnd == -1) {
              // default namespace
              val nsOpt = getReferrer(None)
              val ns = nsOpt match {
                case Some(ns) => ns
                case _ => None
              }
              val localName = name.substring(Preprocessor.MacroStart.length())
              (ns, localName)
            }
            else if (referrerEnd == Preprocessor.MacroStart.length()) {
              // no namespace
              val localName = name.substring(referrerEnd + Preprocessor.ReferrerTerminator.length())
              (None, localName)
            }
            else {
              // referred namespace
              val ref = name.substring(Preprocessor.MacroStart.length(), referrerEnd)
              val nsOpt = getReferrer(Some(ref))
              nsOpt match {
                case Some(ns) => {
                  val localName = name.substring(referrerEnd + Preprocessor.ReferrerTerminator.length())
                  (ns, localName)
                }
                case _ => throw new UnboundReferrerException(ref, src, pos)
              }
            }
          }
          else {
            // explicitly named namespace
            val ns = Some(name.substring(Preprocessor.MacroStart.length(), namespaceEnd))
            val localName = name.substring(namespaceEnd + Preprocessor.MacroStart.length())
            (ns, localName)
          }

          Some(ns.map(decode), decode(localName))
        }
        else
          None
      }

      def handlePrefix(
          name: String,
          expr: Option[Tree] = None,
          args: List[Tree] = List(),
          pos: Position,
        ) = {

        parseQname(name, pos) match {
          case Some((namespaceName, localName)) => {

            getMacro(namespaceName, localName) match {
              case Some(makro) =>
                try {
                  Some(makro(expr, args))
                }
                catch {
                  case e: Exception => throw MacroException(e, namespaceName, localName, src, pos)
                }
              case _ => throw new MacroNotFoundException(namespaceName, localName, src, pos)
            }
          }

          case _ => None
        }

      }

      def extractReferrer(stat: Tree) = {
        stat match {
          case Ident(TermName(name)) => {
            if (name.startsWith(Preprocessor.NamespaceRef)) {
              val pos = name.indexOf(Preprocessor.ReferrerTerminator)

              if (pos == -1) {
                false
              } else {
                setReferrer(Some(name.substring(Preprocessor.NamespaceRef.length(), pos)), Some(name.substring(pos + Preprocessor.ReferrerTerminator.length())))
                true
              }
            }
            else if (name.startsWith(Preprocessor.DefaultNamespace)) {
              setReferrer(None, Some(name.substring(Preprocessor.DefaultNamespace.length())))
              true
            }
            else {
              false
            }
          }
          case _ => {
            false
          }
        }
      }

      def findPropMacro(behind: List[Tree], front: List[Tree]): Option[(Option[String], String, Tree, List[Tree])] = {

        @tailrec
        def zipUp(behind: List[Tree], front: List[Tree]): List[Tree] = {
          behind match {
            case arg::args => zipUp(args, arg::front)
            case _ => front
          }
        }

        front match {
          case ((arg@AssignOrNamedArg(nameExpr@Ident(TermName(argName)), expr))::args) =>
            parseQname(argName, nameExpr.pos) match {
              case Some((nsName, localName)) => Some((nsName, localName, expr, zipUp(behind, args)))
              case _ => findPropMacro(arg::behind, args)
            }
          case (arg::args) => findPropMacro(arg::behind, args)
          case _ => None
        }
      }

      tree match {
        case Ident(TermName(name)) => {
          if (extractReferrer(tree)) q"()"
          else handlePrefix(name, None, List(), tree.pos) match {
            case Some(expr) => transform(expr)
            case _ => super.transform(tree)
          }
        }

        case Apply(expr, args) => {

          // TODO: look for a property macros first
          findPropMacro(List(), args) match {
            case Some((nsName, localName, expr_, args_)) => {
              getMacro(nsName, localName) match {
              case Some(makro) =>
                try {
                  transform(makro(None, List(expr_, Apply(expr, args_))))
                }
                catch {
                  case e: Exception => throw MacroException(e, nsName, localName, src, expr_.pos)
                }
              case _ => throw new MacroNotFoundException(nsName, localName, src, expr_.pos)
              }
            }

            case _ =>
              expr match {
                case Ident(TermName(name)) => {
                  handlePrefix(name, None, args, expr.pos) match {
                    case Some(expr) => transform(expr)
                    case _ => super.transform(tree)
                  }
                }
                case Select(sexpr, TermName(name)) => {
                  handlePrefix(name, Some(sexpr), args, expr.pos) match {
                    case Some(expr) => transform(expr)
                    case _ => super.transform(tree)
                  }
                }
                case _ => super.transform(tree)
              }

          }
        }
        case Block(exprs, expr) => {
          try {
            referrers = referrers match {
              case (head +: tail) => head +: referrers
              case _ => Map[Option[String], Option[String]]() +: referrers
            }
            super.transform(tree)
          }
          finally {
            referrers = referrers.tail
          }
        }
        case _ => {
          super.transform(tree)
        }
      }
    }
  }

  private var src: Option[String] = None;

  def transformMacros(defn: Tree, src: Option[String] = None): Tree = {
    this.src = src

    try {
      transformer.transform(defn)
    }
    finally {
      this.src = None
    }
  }

  def addMacro(
      namespaceName: Option[String] = None,
      localName: Option[String] = None,
      makro: (Option[String], String) => (Option[Tree], List[Tree]) => Tree
    ) = {
    val macOpt = macros.get(namespaceName)
    macOpt match {
      case Some(macrosInNamespace) => {
        val oldMacro = macrosInNamespace.get(localName)
        macrosInNamespace += localName -> makro
        oldMacro
      }
      case _ => {
        macros += namespaceName -> TrieMap(localName -> makro)
        None
      }
    }
  }

  def getMacro(
      namespaceName: Option[String] = None,
      localName: String
    ) : Option[(Option[Tree], List[Tree]) => Tree] =
    macros.get(namespaceName) match {
      case Some(macrosInNamespace) =>
          macrosInNamespace.get(Some(localName)) match {
            case Some(makro) => Some(makro(namespaceName, localName))
            case None => macrosInNamespace.get(None) match {
              case Some(makro) => Some(makro(namespaceName, localName))
              case None => None
            }
          }
      case None => None
    }

  private def namespaceHasMacros(
      namespaceName: Option[String] = None
    ) =
    (macros contains namespaceName)

  def hasMacro(
      namespaceName: Option[String] = None,
      localName: String
    ) = {
    val macrosInNamespace = macros.get(namespaceName)
    macrosInNamespace match {
      case Some(map) => (map contains Some(localName)) || (map contains None)
      case _ => false
    }
  }

  def hasOwnMacro(
      namespaceName: Option[String] = None,
      localName: String
    ) = {
    val macrosInNamespace = macros.get(namespaceName)
    macrosInNamespace match {
      case Some(map) => map contains Some(localName)
      case _ => false
    }
  }
}

object Preprocessor {
  val MacroStart = encode("#")

  val ReferrerTerminator = encode("|")

  val DefaultNamespace = encode("namespace|")

  val NamespaceRef = encode("namespace ")

  def apply() = new Preprocessor
}
