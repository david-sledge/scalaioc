package scala.ioc.xml

import scala.ioc.ppm._
import scala.collection.immutable.Seq
import scala.reflect.runtime.universe._
import scala.xml.Write

package object ppm {
  def f[T](obj: Any) = obj.asInstanceOf[(Any, Write[T])]

  private def metaWriteXml(name: String, args: Seq[Tree]) = {
    val termName = TermName(name)
    List(
      q"""val pair = scala.ioc.xml.ppm.f(c("xmlWriter"))"""
    , q"""val writer = pair._1"""
    , q"""val write = pair._2"""
    , if (args.size == 0) q"""write.$termName(write.cast(writer))"""
      else q"""write.$termName(write.cast(writer))(..$args)"""
    )
  }

  private def postJobContent(args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
    val cdataWrite = q"""..${metaWriteXml("writeCharacters", Seq[Tree](q"""c("cdata").toString"""))}"""
    Right(q"""..${(List[Tree]() /: args)((acc, arg) => q"""
`#scala.ioc#let`("cdata" -> ${arg},
  if (c("cdata") == ()) ()
  else $cdataWrite
)
""" :: acc)}""")
  }

  def postJobXml(namespaceName: Option[String], localName: String)(expr: Option[Tree], args: Seq[Tree])
  : Either[(Boolean, Seq[String]), Tree] = {
    val (argMap, leftovers, _) = mapArgs(Seq(), args)
    postJobContent(leftovers) match {
      case Right(tree) =>
        Right(q"""..${
            (
              if (argMap.contains("version"))
                if (argMap.contains("encoding"))
                  metaWriteXml("writeStartDocumentVerEnc",
                      Seq[Tree](argMap("encoding"), argMap("version")))
                else
                  metaWriteXml("writeStartDocumentVer",
                      Seq[Tree](argMap("version")))
              else if (argMap contains "encoding")
                metaWriteXml("writeStartDocumentVerEnc",
                    Seq[Tree](argMap("encoding"), Literal(Constant("1.0"))))
              else
                metaWriteXml("writeStartDocument",
                    Seq[Tree]())
            ) ++
            (
              (
                if (argMap contains "dtd")
                  List[Tree](q"""write.writeDtd(write.cast(writer))(${argMap("dtd")})""")
                else List()
              ) ++
              List(
                  q"""${toWorker(tree)}(c)"""
                , q"""write.writeEndDocument(write.cast(writer))"""
              )
            )
        }""")
      case l => l
    }

  }

  def postJobDtd(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("dtd"), args)
    Right(q"""..${metaWriteXml("writeDtd", Seq(argMap("dtd")))}""")
  }

  def postJobCdata(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("cdata"), args)
    Right(q"""..${metaWriteXml("writeCdata", Seq(argMap("cdata")))}""")
  }

  def postJobComment(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("cdata"), args)
    Right(q"""..${metaWriteXml("writeComment", Seq(argMap("cdata")))}""")
  }

  def postJobProcInstr(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
    val (argMap, leftovers, unspecified) =
      mapArgs(Seq("target", "data"), args)
    Right(q"""..${
        if (argMap contains "data")
          metaWriteXml("writeProcessingInstructionData",
              Seq(argMap("target"), argMap("data")))
        else
          metaWriteXml("writeProcessingInstruction",
              Seq(argMap("target")))}""")
  }

  def postJobElement(namespaceName: Option[String] = None, localName: String)
  (expr: Option[Tree], args: Seq[Tree]): Either[(Boolean, Seq[String]), Tree] = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq(), args)
    postJobContent(leftovers) match {
      case Right(tree) =>
        Right(q"""..${
            metaWriteXml("writeStartElement", Seq[Tree](Literal(Constant(localName)))) ++
            (List(
                q"""${toWorker(tree)}(c)"""
              , q"""write.writeEndElement(write.cast(writer))"""
            ) /: argMap){
              case (a, (k, v)) =>
                q"""write.writeAttribute(write.cast(writer))($k, $v)""" :: a
            }
        }""")
      case l => l
    }
  }
}
