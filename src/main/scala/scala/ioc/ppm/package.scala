package scala.ioc

import scala.ppm.Preprocessor
import scala.collection.immutable.Seq
import scala.io.Source._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

package object ppm {

  val ScalaIocNamespaceName = "scala.ioc"

  val Worker = "worker"

  val Id = "id"

  def toWorker(stat: Tree) = {
    q"""
(c: scala.collection.immutable.Map[Any, Any]) => $stat
"""
  }

  /**
   * Takes the given arguments and assigns them to the given argument names
   * using the following rules:
   * - named arguments are handled first
   * - ordinal arguments will be assigned to the variable names in the order
   *   they appear
   */
  def mapArgs(
      seqArgNames: Seq[String] = Seq(),
      args: Seq[Tree] = Seq()
    ) = {
    // address the named arguments first, then handle the ordinal arguments
    val (named, ordinal, ordNames) = (args :\ (Map[String, Tree](), List[Tree](), seqArgNames))(
        (t, acc) =>
          acc match {
            case (map, list, ordArgNames) =>
              t match {
                case AssignOrNamedArg(Ident(TermName(name)), expr) => (
                    map + (name -> expr), list, {
                      val ndx = ordArgNames indexOf name
                      if (ndx == -1) ordArgNames else ordArgNames.take(ndx) ++ ordArgNames.drop(ndx + 1)
                    })
                case _ => (map, t :: list, ordArgNames)
              }
            }
        )

    // fill the gaps with the ordinal arguments
    val (named0, ordinal0, ordNames0) = ((named, Seq[Tree](), ordNames) /: ordinal)(
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

    (named0, ordinal0.reverse, ordNames0)
  }

  def populateStaffingMacros(preprocessor: Preprocessor = Preprocessor()) = {

    def postManagerJob(name: String)
        (namespaceName: Option[String], localName: String)
        (exprOpt: Option[Tree], args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
      val (named, _, leftovers) = mapArgs(Seq(Worker), args)
      val missingArgs = if (named contains Worker) Seq() else Seq(Worker)
      exprOpt match {
        case Some(expr) => {
          if (missingArgs.size == 0)
            Right(
                q"""
    factory.${TermName(name)}($expr, ${toWorker(named(Worker))})
    """
            )
          else
            Left((false, missingArgs))
        }
        case _ => Left((true, missingArgs))
      }
    }

    def postRefJob(name: String)
        (namespaceName: Option[String] = None, localName: String)
        (exprOpt: Option[Tree] = None, args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
      val (named, _, leftovers) = mapArgs(Seq(Id), args)
  
      if (named contains Id)
        Right(
            q"""
factory.${TermName(name)}(${named(Id)}, c)
"""
          )
      else
        Left((false, Seq(Id)))
    }

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("="), postManagerJob("setLazyManager"))

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("=>"), postManagerJob("setManager"))

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("ref"), postRefJob("putToWork"))

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("ref!"), postRefJob("crackTheWhip"))

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("let"),
        ((namespaceName, localName) => (expr, args) =>
          Right((args :\ EmptyTree)((block, acc) => {
            acc match {
              case EmptyTree => block
              case _ => q"""${toWorker(acc)}(c + (${block}))""".asInstanceOf[Tree]
            }
          }))))

    preprocessor.addMacro(
        Some(ScalaIocNamespaceName),
        Some("resource"),
        ((namespaceName, localName) => {
          (expr, args) => {
            val (named, _, leftovers) = mapArgs(Seq("path", "encoding"), args);
            named.get("path") match {
              case Some(pathExpr) => {
                val rsrcExpr = named.get("encoding") match {
                  case Some(encExpr) => q"""scala.ioc.ppm.staffFactoryFromResource($pathExpr,
$encExpr, factory = factory, preprocessor = preprocessor)"""
                  case _ => q"""scala.ioc.ppm.staffFactoryFromResource($pathExpr,
factory = factory, preprocessor = preprocessor)"""
                }
                Right(rsrcExpr)
              }
              case _ => Left((false, Seq("path")))
            }
          }
        })
    )

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("def"), ((namespaceName, localName) => (expr, args) => {
              val (named, _, leftovers) = mapArgs(Seq("localName", "defn"), args)
              val nsName = expr match {
                  case None => None
                  case Some(Literal(Constant(ns: String))) => Some(ns)
                  case _ => throw new IllegalArgumentException(
                      "LHS of #" + namespaceName + "#" + localName + " if supplied must be a string literal")
                }

              val localNameOpt = if (named contains "localName")
                  named("localName") match {
                    case Ident(TermName("None")) => None
                    case Literal(Constant(localName: String)) => Some(localName)
                    case Apply(TermName("Some"), Literal(Constant(localName: String))) => Some(localName)
                    case _ => throw new IllegalArgumentException(
                        "'localName' argument must be a string literal or None.  Found:  " + named("localName").getClass)
                  }
                else None

              val defn = if (named contains "defn") {
                  val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
                  tb.compile(named("defn"))().asInstanceOf[(Option[String], String) => (Option[Tree], Seq[Tree])
 => Either[(Boolean, Seq[String]), Tree]]
                }
                else (namespaceName: Option[String], localName: String) => (expr: Option[Tree], args: Seq[Tree]) => Right(q"()")

              preprocessor.addMacro(nsName, localNameOpt, defn)
              Right(q"()")
            }))

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("embed"), ((namespaceName, localName) => (expr, args) => {
              val (named, _, leftovers) = mapArgs(Seq("path", "encoding"), args)
              if (named contains "path")
                named("path") match {
                  case Literal(Constant(path: String)) => {
                    val encoding =
                      if (named contains "encoding")
                        named("encoding") match {
                          case Literal(Constant(encoding: String)) => encoding
                          case _ => throw new IllegalArgumentException(
                            "The optional 'encoding' argument if supplied must be a string literal")
                        }
                      else "utf-8"
                    // obtain toolbox
                    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
                    val code = fromInputStream(getClass.getClassLoader.getResourceAsStream(path), encoding).mkString
                    Right(tb.parse(code))
                  }
                  case _ => throw new IllegalArgumentException(
                      "'path' argument must be a string literal")
                }
              else
                throw new IllegalArgumentException("'path' argument must be provided as a string literal")
            }))

    preprocessor
  }

  // TODO:  scaladoc
  def staffFactory(conf: String, factory: Factory = Factory(),
      preprocessor: Preprocessor = Preprocessor(), src: Option[String] = None): (Factory, Preprocessor) = {
    populateStaffingMacros(preprocessor)
    val code = s"""
(factory: scala.ioc.Factory) =>
  (preprocessor: scala.ppm.Preprocessor) => {$conf}
"""
    scala.ppm.Reader.execute[Factory => Preprocessor => Any](code, preprocessor, src)(factory)(preprocessor)
    (factory, preprocessor)
  }

  // TODO:  scaladoc
  def staffFactoryFromFile(fileName: String, encoding: String = "utf-8", factory: Factory = Factory()
      , preprocessor: Preprocessor = Preprocessor()) =
    staffFactory(fromFile(fileName, encoding).mkString, factory, preprocessor, Some(fileName))

  // TODO:  scaladoc
  def staffFactoryFromResource(path: String, encoding: String = "utf-8", factory: Factory = Factory()
      , preprocessor: Preprocessor = Preprocessor()) =
    staffFactoryFromStream(getClass.getClassLoader.getResourceAsStream(path), encoding, factory, preprocessor, Some(path))

  // TODO:  scaladoc
  def staffFactoryFromStream(stream: java.io.InputStream, encoding: String = "utf-8", factory: Factory = Factory()
      , preprocessor: Preprocessor = Preprocessor(), src: Option[String] = None) =
    staffFactory(fromInputStream(stream, encoding).mkString, factory, preprocessor, src)
}
