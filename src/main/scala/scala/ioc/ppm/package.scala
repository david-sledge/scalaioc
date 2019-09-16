package scala.ioc

import scala.ppm._
import scala.collection.immutable.ListSet
import scala.io.Source._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

package object ppm {

  val ScalaIocNamespaceName = "scala.ioc"

  val Worker = "worker"

  val Id = "id"

  val Type = "type"

  def toWorker(stat: Tree) = {
    q"""
(c: scala.collection.immutable.Map[Any, Any]) => $stat
"""
  }

  def populateStaffingMacros(preprocessor: Preprocessor = Preprocessor()) = {

    def postManagerJob(name: String)
        (namespaceName: Option[String], localName: String)
        (exprOpt: Option[Tree], args: List[Tree]): Tree = {

      val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
          exprOpt,
          args,
          ListSet(Worker),
          thisExprPresence = Required,
        )

      q"""
factory.${TermName(name)}(${exprOpt.get}, ${toWorker(named(Worker))})
"""

    }

    def postRefJob(name: String)
        (namespaceName: Option[String] = None, localName: String)
        (exprOpt: Option[Tree] = None, args: List[Tree]): Tree = {
      val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
          exprOpt,
          args,
          ListSet(Id),
        )

      q"""
factory.${TermName(name)}(${named(Id)}, c)
"""
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
          args.foldRight(EmptyTree)((block, acc) => {
            acc match {
              case EmptyTree => block
              case _ => q"""${toWorker(acc)}(c + (${block}))"""
            }
          })))

    preprocessor.addMacro(
      Some(ScalaIocNamespaceName),
      Some("$"),
      (namespaceName, localName) =>
        (expr, args) => {
          val ProcessedArgs(named, _, extraNames, leftovers) =
            validateThisExprAndArgs(
              expr,
              args,
              ListSet(Id, Type),
            )

          named(Type) match {
            case Ident(TermName(name)) => q"c(${named(Id)}).asInstanceOf[${Ident(TypeName(name))}]"

            case Select(tree, TermName(name)) => q"c(${named(Id)}).asInstanceOf[${Select(tree, TypeName(name))}]"

            case _ => throw new IllegalArgumentException(s"Type argument must be a type")

          }

        }
    )

//*
    preprocessor.addMacro(
        Some(ScalaIocNamespaceName),
        Some("id"),
        (namespaceName, localName) =>
          (expr, args) => {
            val ProcessedArgs(named, _, extraNames, leftovers) =
              validateThisExprAndArgs(
                  expr,
                  args,
                  ListSet(Id, Worker),
                )

            q"""{
var worker = ${toWorker(named(Worker))}
`#$$`("factory", scala.ioc.Factory).setLazyManager($Id, worker)
worker(c)
}"""

          })
// */

    preprocessor.addMacro(
        Some(ScalaIocNamespaceName),
        Some("resource"),
        (namespaceName, localName) =>
          (expr, args) => {
            val ProcessedArgs(named, _, extraNames, leftovers) = validateThisExprAndArgs(
                expr,
                args,
                ListSet("path"),
                ListSet("encoding"),
              )

            val pathExpr = named("path")

            named.get("encoding") match {
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
          (expr, args) => {
            val ProcessedArgs(named, _, extraNames, leftovers) = validateThisExprAndArgs(
                expr,
                args,
                ListSet(),
                ListSet("localName", "defn"),
                thisExprPresence = Optional,
              )

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
                tb.compile(named("defn"))().asInstanceOf[
                  (Option[String], String) =>
                    (Option[Tree], List[Tree]) =>
                      Tree]
              }
              else
                (namespaceName: Option[String], localName: String) =>
                  (expr: Option[Tree], args: List[Tree]) =>
                    // no-op macro
                    q"()"

            preprocessor.addMacro(nsName, localNameOpt, defn)
            q"()"

          })

    preprocessor.addMacro(
        Some(ScalaIocNamespaceName),
        Some("embed"),
        (namespaceName, localName) =>
          (expr, args) => {
            val ProcessedArgs(named, _, extraNames, leftovers) = validateThisExprAndArgs(
                expr,
                args,
                ListSet("path"),
                ListSet("encoding"),
              )
  
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
