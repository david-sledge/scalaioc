package scala.ioc

import ioc._
import scala.collection.immutable.Seq
import scala.meta._
import scala.xml.writer.XmlWriter

package object xml {
  def f[T](obj: Any) = obj.asInstanceOf[(Any, XmlWriter[T])]

  private def xmlWriter(name: Term.Name, args: Seq[Term.Arg]) = {
    List(
      q"""val (writer, typeclassImpl) = scala.ioc.xml.f(c("xmlWriter"))"""
    , if (args.size == 0) q"""typeclassImpl.$name(typeclassImpl.cast(writer))"""
      else q"""typeclassImpl.$name(typeclassImpl.cast(writer))(..$args)"""
    )
  }

  private def postJobContent(args: Seq[Term.Arg]): Term = {
    Term.Block((List[Term]() /: args)((acc, arg) => q"""
`#scala.ioc#let`("cdata" -> ${arg.asInstanceOf[Term]},
  if (c("cdata") == ()) ()
  else ${Term.Block(xmlWriter(q"writeCharacters",
      Seq[Term](q"""c("cdata").toString""")))}
)
""" :: acc))
  }

  def postJobXml(namespaceName: String, localName: String)(expr: Term, args: Seq[Term.Arg])
  : Tree = {
    val (argMap, leftovers, _) = mapArgs(Seq(), args)

    Term.Block(
        (
          if (argMap.contains("version"))
            if (argMap.contains("encoding"))
              xmlWriter(q"writeStartDocumentVerEnc",
                  Seq[Term](argMap("encoding"), argMap("version")))
            else
              xmlWriter(q"writeStartDocumentVer",
                  Seq[Term](argMap("version")))
          else if (argMap contains "encoding")
            xmlWriter(q"writeStartDocumentVerEnc",
                Seq[Term](argMap("encoding"), Lit.String("1.0")))
          else
            xmlWriter(q"writeStartDocument",
                Seq[Term]())
        ) ++
        (
          (
            if (argMap contains "dtd")
              List[Term](q"""typeclassImpl.writeDtd(typeclassImpl.cast(writer))(${argMap("dtd")})""")
            else List()
          ) ++
          List(
              q"""${toWorker(postJobContent(leftovers))}(c)"""
            , q"""typeclassImpl.writeEndDocument(typeclassImpl.cast(writer))"""
          )
        )
    )
  }

  def postJobDtd(namespaceName: String, localName: String)
  (expr: Term, args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("dtd"), args)
    Term.Block(xmlWriter(q"writeDtd", Seq(argMap("dtd"))))
  }

  def postJobCdata(namespaceName: String, localName: String)
  (expr: Term, args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("cdata"), args)
    Term.Block(xmlWriter(q"writeCdata", Seq(argMap("cdata"))))
  }

  def postJobComment(namespaceName: String, localName: String)
  (expr: Term, args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("cdata"), args)
    Term.Block(xmlWriter(q"writeComment", Seq(argMap("cdata"))))
  }

  def postJobProcInstr(namespaceName: String, localName: String)
  (expr: Term, args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) =
      mapArgs(Seq("target", "data"), args)
    Term.Block(
        if (argMap contains "data")
          xmlWriter(q"writeProcessingInstructionData",
              Seq(argMap("target"), argMap("data")))
        else
          xmlWriter(q"writeProcessingInstruction",
              Seq(argMap("target"))))
  }

  def postJobElement(namespaceName: String, localName: String)
  (expr: Term, args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq(), args)
    Term.Block(
        xmlWriter(q"writeStartElement", Seq[Term](Lit.String(localName))) ++
        (List(
            q"""${toWorker(postJobContent(leftovers))}(c)"""
          , q"""typeclassImpl.writeEndElement(typeclassImpl.cast(writer))"""
        ) /: argMap){
          case (a, (k, v)) =>
            q"""typeclassImpl.writeAttribute(typeclassImpl.cast(writer))($k, $v)""" :: a
        }
    )
  }
}
