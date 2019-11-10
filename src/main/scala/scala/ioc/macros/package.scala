package scala.ioc

import scala.language.experimental.{macros => smacros}
import scala.reflect.macros.whitebox.Context

package object macros {

  import scala.io.Source._

  val DefaultEnc = "utf-8"

  def refImpl(c: Context)(id: c.Tree, name: String) = {

    import c.universe._

    q"scala.ioc.cast(factory.${TermName(name)}($id, c))"

  }

  def refStrictImpl(c: Context)(id: c.Tree) = {

    import c.universe._

    refImpl(c)(id, "crackTheWhip")

  }

  def refF(id: Any): Any = macro refStrictImpl

  def refLazyImpl(c: Context)(id: c.Tree) = {

    import c.universe._

    refImpl(c)(id, "putToWork")

  }

  def ref(id: Any): Any = macro refLazyImpl

  def vrefImpl(c: Context)(id: c.Tree) = {

    import c.universe._

    q"scala.ioc.cast(c($id))"

  }

  def $(id: Any): Any = macro vrefImpl

  def embedImpl(c: Context)(path: c.Tree, enc: c.Tree) = {

    import c.universe._

    path match {
      case Literal(Constant(path: String)) => {
        val encoding =
          enc match {
            case Literal(Constant(encoding: String)) => encoding
            case _ => throw new IllegalArgumentException(
              "The optional 'encoding' argument if supplied must be a string literal")
          }

        val code = fromInputStream(this.getClass.getClassLoader.getResourceAsStream(path), encoding).mkString

        try {
          c.parse(code)
        }
        catch {
          case e: Exception => throw new Exception(path, e)
        }
      }
      case _ => throw new IllegalArgumentException(
          "'path' argument must be a string literal")
    }

  }

  def embed(path: String, enc: String = DefaultEnc): Any = macro embedImpl

  def resourceImpl(c: Context)(path: c.Tree, enc: c.Tree) = {

    import c.universe._

    q"scala.ioc.macros.staffFactoryFromResource($path, $enc, factory)"

  }

  def resource(path: String, enc: String = DefaultEnc): Factory = macro resourceImpl

  def staffFactory(
    code: String,
    factory: Factory = Factory(),
    src: Option[String] = None,
  ) = {

    import scala.ioc._
    import scala.tools.reflect.ToolBox
    import scala.reflect.runtime.universe._

    // obtain toolbox
    val tb = runtimeMirror(this.getClass().getClassLoader).mkToolBox(options = "")

    try {
      // generate the AST
      val tree = tb.parse(
        s"(factory: scala.ioc.Factory) => {$code}"
      )
      // compile and execute
      tb.compile(tree)().asInstanceOf[Factory => _](factory)
    }
    catch {
      case e: Exception => src match {
        case Some(src) => throw new Exception(src, e)
        case _ => throw e
      }
    }
    factory

  }

  def staffFactoryFromFile(
    fileName: String,
    encoding: String = DefaultEnc,
    factory: Factory = Factory(),
  ) = staffFactory(
      fromFile(fileName, encoding).mkString,
      factory,
      Some(fileName),
    )

  def staffFactoryFromStream(
    stream: java.io.InputStream,
    encoding: String = DefaultEnc,
    factory: Factory = Factory(),
    src: Option[String] = None,
  ) = staffFactory(fromInputStream(stream, encoding).mkString, factory, src)

  def staffFactoryFromResource(
    path: String,
    enc: String = DefaultEnc,
    factory: Factory = Factory(),
  ) = staffFactoryFromStream(
      this.getClass.getClassLoader.getResourceAsStream(path),
      enc,
      factory,
      Some(path),
    )

  object WishList {

    import scala.language.experimental.{macros => smacros}
    import scala.reflect.macros.whitebox.Context

    def toWorker(c: Context)(stat: c.Tree) = {

      import c.universe._

      q"""
  (c: scala.collection.immutable.Map[Any, Any]) => $stat.asInstanceOf[Any]
  """
    }

    def assign(c: Context)(work: c.Tree, methodName: String) = {

      import c.universe._

      q"""
  scala.ioc.Factory.${TermName(methodName)}(factory, ${c.prefix}.id, ${toWorker(c)(work)})
  """

    }

    def assignImpl(c: Context)(work: c.Tree) = {

      import c.universe._

      assign(c)(work, "setManager")

    }

    def assignThunkImpl(c: Context)(work: c.Tree) = {

      import c.universe._

      assign(c)(work, "setLazyManager")

    }

    implicit class Extension(val id: Any) extends AnyVal {
      def *=>(work: Any): Unit = macro assignImpl
      def *=(work: Any): Unit = macro assignThunkImpl
    }

    def letImpl(c: Context)(lets: c.Tree*)(last: c.Tree) = {

      import c.universe._

      lets.foldLeft(last)(
        (acc, block) => q"""${toWorker(c)(acc)}(c + $block)"""
      )
    }

    def let(lets: (Any, Any)*)(last: Any): Any = macro letImpl
  }

}
