package scala.ioc

import scala.ppm._
import scala.collection.immutable.ListSet
import scala.io.Source._
import scala.reflect.runtime.universe
  import universe._
import scala.tools.reflect.ToolBox

package object ppm {

  val DefaultEnc = "utf-8"

  val ScalaIocNamespaceName = "scalaioc"

  def toWorker(stat: Tree) = {
    q"""
(c: scala.collection.immutable.Map[Any, Any]) => scala.ioc.cast[Any]($stat)
"""
  }

  def populateStaffingMacros(preprocessor: Preprocessor = Preprocessor()) = {

    val worker = "worker"
    val id = "id"
    val lazyMgrMethod = "setLazyManager"
    val mgrMethod = "setManager"
    val path = "path"
    val enc = "encoding"

    def postManagerJob(name: String)
        (namespaceName: Option[String], localName: String)
        (macroArgs: MacroArgs): Tree = {

      val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
      val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
          exprOpt,
          args,
          ListSet(worker),
          thisExprPresence = Required,
        )

      q"""
scala.ioc.Factory.${TermName(name)}(factory, ${exprOpt.get}, ${toWorker(named(worker))})
"""

    }

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("="), postManagerJob(lazyMgrMethod))

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("=>"), postManagerJob(mgrMethod))

    def postRefJob(name: String)
        (namespaceName: Option[String] = None, localName: String)
        (macroArgs: MacroArgs): Tree = {

      val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
      val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
          exprOpt,
          args,
          ListSet(id),
        )

      val rargs = List(q"factory.${TermName(name)}(${named(id)}, c)")
      val rexpr = q"scala.ioc.cast"

      targs match {
        case targ::rest => Apply(TypeApply(rexpr, List(targ)), rargs)
        case _ => Apply(rexpr, rargs)
      }
    }

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("ref"), postRefJob("putToWork"))

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("ref!"), postRefJob("crackTheWhip"))

    preprocessor.addMacro(Some(ScalaIocNamespaceName),
        Some("let"),
        ((namespaceName, localName) => (macroArgs) => {

          val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs

          args.foldRight(EmptyTree)((block, acc) => {
            acc match {
              case EmptyTree => block
              case _ => q"""${toWorker(acc)}(c + (${block}))"""
            }
          })

        })
    )

    preprocessor.addMacro(
      Some(ScalaIocNamespaceName),
      Some("$"),
      (namespaceName, localName) =>
        (macroArgs) => {

          val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
          val ProcessedArgs(named, _, _, _) =
            validateThisExprAndArgs(
              exprOpt,
              args,
              ListSet(id),
            )

          val rargs = List(q"c(${named(id)})")
          val rexpr = q"scala.ioc.cast"

          targs match {
            case targ::rest => Apply(TypeApply(rexpr, List(targ)), rargs)
            case _ => Apply(rexpr, rargs)
          }

        }
    )

    preprocessor.addMacro(
        Some(ScalaIocNamespaceName),
        Some("resource"),
        (namespaceName, localName) =>
          (macroArgs) => {

            val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
            val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
                exprOpt,
                args,
                ListSet(path),
                ListSet(enc),
              )

            val pathExpr = named(path)

            named.get(enc) match {
              case Some(encExpr) => q"""scala.ioc.ppm.staffFactoryFromResource($pathExpr,
$encExpr, factory = factory, preprocessor = preprocessor)"""
              case _ => q"""scala.ioc.ppm.staffFactoryFromResource($pathExpr,
factory = factory, preprocessor = preprocessor)"""
            }

          }
    )

    preprocessor.addMacro(
        Some(ScalaIocNamespaceName),
        Some("def"),
        (namespaceName, localName) =>
          (macroArgs) => {

            val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
            val localName = "localName"
            val defn = "defn"
            val ns = "ns"
            val ProcessedArgs(named, _, _, leftovers) = validateThisExprAndArgs(
              exprOpt,
              args.reverse,
              ListSet(defn),
              ListSet(localName, ns),
            )

            val nsName = named.get(ns) match {
              case None => None
              case Some(Literal(Constant(ns: String))) => Some(ns)
              case _ => throw new IllegalArgumentException(
                  s"'ns' argument if supplied must be a string literal Found ${named(ns)}")
            }

            val localNameOpt = named.get(localName) match {
              case None => None
              case Some(Literal(Constant(localName: String))) => Some(localName)
              case _ => throw new IllegalArgumentException(
                  s"'localName' argument if supplied must be a string literal. Found ${named(localName)}")
            }

            val defnTree = tb.compile(named(defn))().asInstanceOf[
                  (Option[String], String) =>
                    MacroArgs =>
                      Tree]

            preprocessor.addMacro(nsName, localNameOpt, defnTree)
            q"()"

          })

    preprocessor.addMacro(
        Some(ScalaIocNamespaceName),
        Some("embed"),
        (namespaceName, localName) =>
          (macroArgs) => {

            val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
            val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
                exprOpt,
                args,
                ListSet(path),
                ListSet(enc),
              )
  
            named(path) match {
              case Literal(Constant(path: String)) => {
                val encoding =
                  if (named contains enc)
                    named(enc) match {
                      case Literal(Constant(encoding: String)) => encoding
                      case _ => throw new IllegalArgumentException(
                        "The optional 'encoding' argument if supplied must be a string literal")
                    }
                  else DefaultEnc

                val code = fromInputStream(getClass.getClassLoader.getResourceAsStream(path), encoding).mkString
                tb.parse(code)
              }
              case _ => throw new IllegalArgumentException(
                  "'path' argument must be a string literal")
            }

          })

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
    execute[Factory => Preprocessor => Any](code, preprocessor, src)(factory)(preprocessor)
    (factory, preprocessor)
  }

  // TODO:  scaladoc
  def staffFactoryFromFile(fileName: String, encoding: String = DefaultEnc, factory: Factory = Factory()
      , preprocessor: Preprocessor = Preprocessor()) =
    staffFactory(fromFile(fileName, encoding).mkString, factory, preprocessor, Some(fileName))

  // TODO:  scaladoc
  def staffFactoryFromResource(path: String, encoding: String = DefaultEnc, factory: Factory = Factory()
      , preprocessor: Preprocessor = Preprocessor()) =
    staffFactoryFromStream(getClass.getClassLoader.getResourceAsStream(path), encoding, factory, preprocessor, Some(path))

  // TODO:  scaladoc
  def staffFactoryFromStream(stream: java.io.InputStream, encoding: String = DefaultEnc, factory: Factory = Factory()
      , preprocessor: Preprocessor = Preprocessor(), src: Option[String] = None) =
    staffFactory(fromInputStream(stream, encoding).mkString, factory, preprocessor, src)
}
