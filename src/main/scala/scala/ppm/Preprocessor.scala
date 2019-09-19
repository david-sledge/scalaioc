package scala.ppm

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Map
import scala.reflect.runtime.universe
  import universe._
import scala.reflect.NameTransformer._
import scala.tools.reflect.ToolBox

final class Preprocessor {

  // this processor's macros
  private val macros = TrieMap[
    Option[String],
    TrieMap[
      Option[String],
      NamespacedMacro,
    ]
  ]()

  def transformTree(defn: Tree, tb: ToolBox[universe.type], src: Option[String] = None): Tree = {

    (new Transformer() {

      var referrers = List[Map[Option[String], Option[String]]]()

      def resolveReferrer[T](ref: Option[String], onExists: Option[String] => T, onNotExists: => T,// */
        ) = {
        referrers match {
          case head +: tail => {
            head.get(ref) match {
              case Some(nsName) => onExists(nsName)
              case _ => onNotExists
            }
          }
          case _ => onNotExists
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

        def parseQname[T](
            name: String,
            pos: Position,
            onIsQname: (Option[String], String) => T,
            onIsNotQname: => T,
          ) = {

          if (name.startsWith(Preprocessor.MacroStart)) {

            val namespaceEnd = name.indexOf(Preprocessor.MacroStart, Preprocessor.MacroStart.length())

            val (ns, localName) = if (namespaceEnd == -1) {
              val referrerEnd = name.indexOf(Preprocessor.ReferrerTerminator)

              if (referrerEnd == -1) {

                // default namespace
                (
                  // namespace name
                  resolveReferrer(
                    None,
                    onExists = (ns) => ns,
                    onNotExists = None,
                  ),

                  // local name
                  name.substring(Preprocessor.MacroStart.length())
                )

              }
              else if (referrerEnd == Preprocessor.MacroStart.length()) {
                // no namespace
                val localName = name.substring(referrerEnd + Preprocessor.ReferrerTerminator.length())
                (None, localName)
              }
              else {

                // referred namespace
                val ref = name.substring(Preprocessor.MacroStart.length(), referrerEnd)
                resolveReferrer(
                    Some(ref),
                    onExists = (ns) => {
                      val localName = name.substring(referrerEnd + Preprocessor.ReferrerTerminator.length())
                      (ns, localName)
                    },
                    onNotExists = throw new UnboundReferrerException(ref, src, pos),
                  )

              }
            }
            else {
              // explicitly named namespace
              val ns = Some(name.substring(Preprocessor.MacroStart.length(), namespaceEnd))
              val localName = name.substring(namespaceEnd + Preprocessor.MacroStart.length())
              (ns, localName)
            }

            onIsQname(ns.map(decode), decode(localName))
          }
          else
            onIsNotQname
        }

        def getAndApplyMacro[T](
            nsName: Option[String],
            localName: String,
            exprOpt: Option[Tree],
            args: List[Tree],
            pos: Position,
            processApplication: Tree => T,
          ) = {

          getMacro(
              nsName,

              localName,

              onExists = (makro) =>
                try {
                  processApplication(makro(exprOpt, args, tb, src))
                }
                catch {
                  case e: Exception => throw MacroException(e, nsName, localName, src, pos)
                },

              onNotExists = throw new MacroNotFoundException(nsName, localName, src, pos),
            )

        }

        def handlePrefix(
            name: String,
            srcTree: Tree,
            expr: Option[Tree] = None,
            args: List[Tree] = List(),
            pos: Position,
          ) = {

          parseQname(
              name,
              pos,

              onIsQname = (namespaceName, localName) =>
                getAndApplyMacro(
                    namespaceName,
                    localName,
                    expr,
                    args,
                    pos,
                    transform
                  ),

              onIsNotQname = super.transform(srcTree),

            )

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

        def findPropMacro[T](
            behind: List[Tree],
            front: List[Tree],
            onFound: (Option[String], String, Tree, List[Tree]) => T,
            onNotFound: => T,
          ): T = {

          @tailrec
          def zipUp(behind: List[Tree], front: List[Tree]): List[Tree] = {
            behind match {
              case arg::args => zipUp(args, arg::front)
              case _ => front
            }
          }

          front match {
            case ((arg@AssignOrNamedArg(nameExpr@Ident(TermName(argName)), expr))::args) =>
              parseQname(
                  argName,
                  nameExpr.pos,
                  onIsQname = (nsName, localName) => onFound(nsName, localName, expr, zipUp(behind, args)),
                  onIsNotQname = findPropMacro(arg::behind, args, onFound, onNotFound)
                )
            case (arg::args) => findPropMacro(arg::behind, args, onFound, onNotFound)
            case _ => onNotFound
          }
        }

        tree match {
          case Ident(TermName(name)) => {
            if (extractReferrer(tree)) q"()"
            else handlePrefix(name, tree, None, List(), tree.pos)
          }

          case Apply(expr, args) => {

            findPropMacro(
                List(),
                args,
                onFound = (nsName, localName, expr_, args_) => {

                  getAndApplyMacro(
                      nsName,
                      localName,
                      None,
                      List(expr_, Apply(expr, args_)),
                      expr_.pos,
                      transform,
                    )
                },
                onNotFound =
                  expr match {
                    case Ident(TermName(name)) => {
                      handlePrefix(name, tree, None, args, expr.pos)
                    }
                    case Select(sexpr, TermName(name)) => {
                      handlePrefix(name, tree, Some(sexpr), args, expr.pos)
                    }
                    case _ => super.transform(tree)
                  },
              )
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
    }).transform(defn)

  }

  def addMacro(
      namespaceName: Option[String] = None,
      localName: Option[String] = None,
      makro: NamespacedMacro,
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

  def getMacro[T](
      namespaceName: Option[String] = None,
      localName: String,
      onExists: Macro => T,
      onNotExists: => T,
    ): T =
    macros.get(namespaceName) match {
      case Some(macrosInNamespace) =>
          macrosInNamespace.get(Some(localName)) match {
            case Some(makro) => onExists(makro(namespaceName, localName))
            case _ => macrosInNamespace.get(None) match {
              case Some(makro) => onExists(makro(namespaceName, localName))
              case _ => onNotExists
            }
          }
      case None => onNotExists
    }

  private def namespaceHasMacros(
      namespaceName: Option[String] = None,
    ) =
    (macros contains namespaceName)

  def hasMacro(
      namespaceName: Option[String] = None,
      localName: String,
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
