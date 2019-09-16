package scala.ioc.xml

import scala.ioc.ppm._
import scala.collection.immutable.ListSet
import scala.ppm._
import scala.reflect.runtime.universe._
import scala.xml.Write

package object ppm {
  def f[T](obj: Any) = obj.asInstanceOf[(Any, Write[T])]

  private def metaWriteXml(name: String, args: List[Tree]) = {
    val termName = TermName(name)
    List(
      q"""val pair = scala.ioc.xml.ppm.f(c("xmlWriter"))"""
    , q"""val writer = pair._1"""
    , q"""val write = pair._2"""
    , if (args.size == 0) q"""write.$termName(write.cast(writer))"""
      else q"""write.$termName(write.cast(writer))(..$args)"""
    )
  }

  private def postJobContent(args: List[Tree]): Tree = {
    val cdataWrite = q"""..${metaWriteXml("writeCharacters", List[Tree](q"""c("cdata").toString"""))}"""
    q"""..${args.foldLeft(List[Tree]())((acc, arg) => q"""
`#scala.ioc#let`("cdata" -> ${arg},
  if (c("cdata") == ()) ()
  else $cdataWrite
)
""" :: acc)}"""
  }

  def postJobXml(
      namespaceName: Option[String],
      localName: String
    )(expr: Option[Tree],
      args: List[Tree]
    ): Tree = {

    val ProcessedArgs(named, _, _, leftovers) =
      validateThisExprAndArgs(
          expr,
          args,
          allowExtraNamedArgs = true,
          allowExcessOrdinalArgs = true,
        )

    q"""..${
        (
          if (named.contains("version"))
            if (named.contains("encoding"))
              metaWriteXml("writeStartDocumentVerEnc",
                  List[Tree](named("encoding"), named("version")))
            else
              metaWriteXml("writeStartDocumentVer",
                  List[Tree](named("version")))
          else if (named contains "encoding")
            metaWriteXml("writeStartDocumentVerEnc",
                List[Tree](named("encoding"), Literal(Constant("1.0"))))
          else
            metaWriteXml("writeStartDocument",
                List[Tree]())
        ) ++
        (
          (
            if (named contains "dtd")
              List[Tree](q"""write.writeDtd(write.cast(writer))(${named("dtd")})""")
            else List()
          ) ++
          List(
              q"""${toWorker(postJobContent(leftovers.right.get))}(c)"""
            , q"""write.writeEndDocument(write.cast(writer))"""
          )
        )
    }"""

  }

  def postJobDtd(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: List[Tree]): Tree = {

    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
        expr,
        args,
        ListSet("dtd"),
      )

    q"""..${metaWriteXml("writeDtd", List(named("dtd")))}"""

  }

  def postJobCdata(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: List[Tree]): Tree = {

    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
        expr,
        args,
        ListSet("cdata"),
      )

    q"""..${metaWriteXml("writeCdata", List(named("cdata")))}"""

  }

  def postJobComment(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: List[Tree]): Tree = {

    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
        expr,
        args,
        ListSet("cdata"),
      )

    q"""..${metaWriteXml("writeComment", List(named("cdata")))}"""

  }

  def postJobProcInstr(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: List[Tree]): Tree = {

    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
        expr,
        args,
        ListSet("target"),
        ListSet("data"),
      )

    q"""..${
      if (named contains "data")
        metaWriteXml("writeProcessingInstructionData",
            List(named("target"), named("data")))
      else
        metaWriteXml("writeProcessingInstruction",
            List(named("target")))}"""

  }

  def postJobElement(namespaceName: Option[String] = None, localName: String)
  (expr: Option[Tree], args: List[Tree]): Tree = {
    val exprIsPresent = expr != None
    val ProcessedArgs(named, _, _, leftovers) =
      validateThisExprAndArgs(
          expr,
          args,
          allowExtraNamedArgs = true,
          allowExcessOrdinalArgs = true,
        )

    q"""..${
        metaWriteXml("writeStartElement", List[Tree](Literal(Constant(localName)))) ++
        named.foldLeft(List(
            q"""${toWorker(postJobContent(leftovers.right.get))}(c)"""
          , q"""write.writeEndElement(write.cast(writer))"""
        )){
          case (a, (k, v)) =>
            q"""write.writeAttribute(write.cast(writer))($k, $v)""" :: a
        }
    }"""
  }
}
