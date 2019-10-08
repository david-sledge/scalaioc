package scala.ioc

import scala.ppm._
import scala.collection.immutable.ListSet
import scala.io.Source._
import scala.reflect.runtime.universe
  import universe._
import scala.tools.reflect.ToolBox

package object ppm {

  val DefaultEnc = "utf-8"

  def toWorker(stat: Tree) = {
    q"""
(c: scala.collection.immutable.Map[Any, Any]) => $stat
"""
  }

  def populateStaffingMacros(preprocessor: Preprocessor = Preprocessor()) = {

    val scalaIocNamespaceName = "scala.ioc"
    val worker = "worker"
    val id = "id"
    val lazyMgrMethod = "setLazyManager"
    val mgrMethod = "setManager"
    val path = "path"
    val enc = "encoding"

    def postManagerJob(name: String)
        (namespaceName: Option[String], localName: String)
        (exprOpt: Option[Tree], args: List[Tree], tb: ToolBox[universe.type], src: Option[String]): Tree = {

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

    preprocessor.addMacro(Some(scalaIocNamespaceName),
        Some("="), postManagerJob(lazyMgrMethod))

    preprocessor.addMacro(Some(scalaIocNamespaceName),
        Some("=>"), postManagerJob(mgrMethod))

    def postRefJob(name: String)
        (namespaceName: Option[String] = None, localName: String)
        (exprOpt: Option[Tree] = None, args: List[Tree], tb: ToolBox[universe.type], src: Option[String]): Tree = {
      val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
          exprOpt,
          args,
          ListSet(id),
        )

      q"""
factory.${TermName(name)}(${named(id)}, c)
"""
    }

    preprocessor.addMacro(Some(scalaIocNamespaceName),
        Some("ref"), postRefJob("putToWork"))

    preprocessor.addMacro(Some(scalaIocNamespaceName),
        Some("ref!"), postRefJob("crackTheWhip"))

    preprocessor.addMacro(Some(scalaIocNamespaceName),
        Some("let"),
        ((namespaceName, localName) => (expr, args, tb, src) =>
          args.foldRight(EmptyTree)((block, acc) => {
            acc match {
              case EmptyTree => block
              case _ => q"""${toWorker(acc)}(c + (${block}))"""
            }
          })))

    preprocessor.addMacro(
      Some(scalaIocNamespaceName),
      Some("$"),
      (namespaceName, localName) =>
        (expr, args, tb, src) => {
          val ProcessedArgs(named, _, _, _) =
            validateThisExprAndArgs(
              expr,
              args,
              ListSet(id),
            )

          q"scala.ioc.cast(c(${named(id)}))"

        }
    )

    def postManagementPromotion(name: String)
        (namespaceName: Option[String], localName: String)
        (exprOpt: Option[Tree], args: List[Tree], tb: ToolBox[universe.type], src: Option[String]): Tree = {

      val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
          exprOpt,
          args,
          ListSet(id, worker),
        )

      q"""{
var worker = ${toWorker(named(worker))}
scala.ioc.Factory.${TermName(name)}(factory, ${named(id)}, worker)
worker(c)
}"""

    }

    preprocessor.addMacro(Some(scalaIocNamespaceName),
        Some("id"), postManagementPromotion(lazyMgrMethod))

    preprocessor.addMacro(Some(scalaIocNamespaceName),
        Some("id>"), postManagementPromotion(mgrMethod))

    preprocessor.addMacro(
        Some(scalaIocNamespaceName),
        Some("resource"),
        (namespaceName, localName) =>
          (expr, args, tb, src) => {
            val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
                expr,
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
        Some(scalaIocNamespaceName),
        Some("def"),
        (namespaceName, localName) =>
          (expr, args, tb, src) => {
            val localName = "localName"
            val defnArg = "defn"
            val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
                expr,
                args,
                ListSet(),
                ListSet(localName, defnArg),
                thisExprPresence = Optional,
              )

            val nsName = expr match {
              case None => None
              case Some(Literal(Constant(ns: String))) => Some(ns)
              case _ => throw new IllegalArgumentException(
                  s"LHS of #$namespaceName#$localName if supplied must be a string literal")
            }

            val localNameOpt = if (named contains localName)
              named(localName) match {
                case Ident(TermName("None")) => None
                case Literal(Constant(localName: String)) => Some(localName)
                case Apply(TermName("Some"), Literal(Constant(localName: String))) => Some(localName)
                case _ => throw new IllegalArgumentException(
                    s"'localName' argument must be a string literal or None. Found: ${named(localName).getClass}")
              }
            else None

            val defn = if (named contains defnArg) {

                tb.compile(named(defnArg))().asInstanceOf[
                  (Option[String], String) =>
                    (Option[Tree], List[Tree], ToolBox[universe.type], Option[String]) =>
                      Tree]
              }
              else
                (namespaceName: Option[String], localName: String) =>
                  (expr: Option[Tree], args: List[Tree], tb: ToolBox[universe.type], src: Option[String]) =>
                    // no-op macro
                    q"()"

            preprocessor.addMacro(nsName, localNameOpt, defn)
            q"()"

          })

    preprocessor.addMacro(
        Some(scalaIocNamespaceName),
        Some("embed"),
        (namespaceName, localName) =>
          (expr, args, tb, src) => {
            val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
                expr,
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
