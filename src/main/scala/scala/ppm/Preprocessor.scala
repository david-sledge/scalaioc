package scala.ppm

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Map
import scala.collection.immutable.Seq
import scala.reflect.runtime.universe._
import scala.reflect.NameTransformer._

final class Preprocessor {

  private val macros = TrieMap[Option[String], TrieMap[Option[String],
      (Option[String], String) => (Option[Tree], Seq[Tree]) => Either[(Boolean, Seq[String]), Tree]]]()

  private val transformer = new Transformer() {

    var referrers2 = Seq[Map[Option[String], Option[String]]]()

    def getReferrer(ref: Option[String]) = {
      referrers2 match {
        case head +: tail => {
          head.get(ref)
        }
        case _ => None
      }
    }

    def setReferrer(ref: Option[String], namespaceName: Option[String]) = {
      referrers2 = referrers2 match {
        case head +: tail => {
          (head + (ref -> namespaceName)) +: tail
        }
        case _ => referrers2
      }
    }

    override def transform(tree: Tree): Tree = {

      def handlePrefix(
          name: String,
          expr: Option[Tree] = None,
          args: Seq[Tree] = Seq()
        ): MacroResult[Option[Tree]] =
      {
        if (name.startsWith(Preprocessor.MacroStart)) {
          val qnameEith: Either[String, (Option[String], String)] = {
            val namespaceEnd = name.indexOf(Preprocessor.MacroStart, Preprocessor.MacroStart.length())

            if (namespaceEnd == -1) {
              val referrerEnd = name.indexOf(Preprocessor.ReferrerTerminator)

              if (referrerEnd == -1) {
                // default namespace
                val nsOpt = getReferrer(None)
                val ns = nsOpt match {
                  case Some(ns) => ns
                  case _ => None
                }
                val localName = name.substring(Preprocessor.MacroStart.length())
                Right((ns, localName))
              }
              else if (referrerEnd == Preprocessor.MacroStart.length()) {
                // no namespace
                val localName = name.substring(referrerEnd + Preprocessor.ReferrerTerminator.length())
                Right((None, localName))
              }
              else {
                // referred namespace
                val ref = name.substring(Preprocessor.MacroStart.length(), referrerEnd)
                val nsOpt = getReferrer(Some(ref))
                nsOpt match {
                  case Some(ns) => {
                    val localName = name.substring(referrerEnd + Preprocessor.ReferrerTerminator.length())
                    Right((ns, localName))
                  }
                  case _ => Left(ref)
                }
              }
            }
            else {
              // explicitly named namespace
              val ns = Some(name.substring(Preprocessor.MacroStart.length(), namespaceEnd))
              val localName = name.substring(namespaceEnd + Preprocessor.MacroStart.length())
              Right((ns, localName))
            }
          }

          qnameEith match {
            case Left(ref) => UnboundReferrer(ref)
            case Right((namespaceName, localName)) => {
              val decodedNamespaceName = namespaceName.map(decode)
              val decodedLocalName = decode(localName)
              val macroOpt = getMacro(decodedNamespaceName, decodedLocalName)
              macroOpt match {
                case Some(makro) => makro(expr, args) match {
                  case Right(tree) => Ok(Some(tree))
                  case Left((isMissingRequiredObj, missingRequiredArgs)) => Error(decodedNamespaceName, decodedLocalName, isMissingRequiredObj, missingRequiredArgs)
                }
                case _ => MacroNotFound(decodedNamespaceName, decodedLocalName)
              }
            }
          }
        }
        else Ok(None)
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

      def asyncHandlePrefix(
          name: String,
          expr: Option[Tree] = None,
          args: Seq[Tree],
          pos: Position
        ) = {

        val result =
          try {
            handlePrefix(name, expr, args)
          }
          catch {
            case e: Exception => throw MacroException(e, src, pos)
          }

        result match {
          case MacroNotFound(ns, localName) => throw new MacroNotFoundException(ns, localName, src, pos)
          case UnboundReferrer(ref) => throw new UnboundReferrerException(ref, src, pos)
          case Error(namespaceName, localName, isMissingRequiredObj, missingRequiredArgs) => throw new MissingRequiredObjectOrArguments(namespaceName, localName, isMissingRequiredObj, missingRequiredArgs, src, pos)
          case Ok(expr) => expr
        }
      }

      tree match {
        case Ident(TermName(name)) => {
          if (extractReferrer(tree)) q"()"
          else asyncHandlePrefix(name, None, List(), tree.pos) match {
            case Some(expr) => transform(expr)
            case _ => super.transform(tree)
          }
        }
        case Apply(expr, args) => {
          expr match {
            case Ident(TermName(name)) => {
              asyncHandlePrefix(name, None, args, expr.pos) match {
                case Some(expr) => transform(expr)
                case _ => super.transform(tree)
              }
            }
            case Select(sexpr, TermName(name)) => {
              asyncHandlePrefix(name, Some(sexpr), args, expr.pos) match {
                case Some(expr) => transform(expr)
                case _ => super.transform(tree)
              }
            }
            case _ => super.transform(tree)
          }
        }
        case Block(exprs, expr) => {
          try {
            referrers2 = referrers2 match {
              case (head +: tail) => head +: referrers2
              case _ => Map[Option[String], Option[String]]() +: referrers2
            }
            super.transform(tree)
          }
          finally {
            referrers2 = referrers2.tail
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
      makro: (Option[String], String) => (Option[Tree], Seq[Tree]) => Either[(Boolean, Seq[String]), Tree]
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
    ) : Option[(Option[Tree], Seq[Tree]) => Either[(Boolean, Seq[String]), Tree]] =
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
