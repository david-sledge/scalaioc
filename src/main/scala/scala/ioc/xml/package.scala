package scala.ioc

import ioc._
import scala.collection.immutable.Seq
import scala.meta._
import scala.xml.stream.XmlWriterTypeclass

package object xml {
  def f[T](obj: Any) = obj.asInstanceOf[(Any, XmlWriterTypeclass[T])]

  private def xmlWriter(name: Term.Name, args: Seq[Term.Arg]) = {
    List(
      q"""val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))"""
    , q"""typeclass.$name(typeclass.cast(writer), ..$args)"""
    )
  }

  private def postJobContent(args: Seq[Term.Arg]): Term = {
    Term.Block(args.foldLeft(List[Term]())((acc, arg) => q"""
`#scala.ioc#let`("cdata", $arg,
  if (c("cdata") == ()) ()
  else ${Term.Block(xmlWriter(q"writeCharacters",
      Seq[Term](q"""c("cdata").toString""")))}
)
""" :: acc))
  }

  def postJobXml(namespaceName: String, localName: String)(args: Seq[Term.Arg])
  : Tree = {
    val (argMap, leftovers, _) = mapArgs(Seq(), args)

    Term.Block(
        xmlWriter(q"writeStartDocument",
            if (argMap.contains("version"))
              if (argMap.contains("encoding"))
                Seq[Term](argMap("encoding"), argMap("version"))
              else
                Seq[Term](argMap("version"))
            else if (argMap contains "encoding")
              Seq[Term](argMap("encoding"), Lit.String("1.0"))
            else
              Seq[Term]()
          ) ++
        (
          (
            if (argMap contains "dtd")
              List[Term](q"""typeclass.writeDTD(typeclass.cast(writer), ${argMap("dtd")})""")
            else List()
          ) ++
          List(
              q"""${toWorker(postJobContent(leftovers))}(c)"""
            , q"""typeclass.writeEndDocument(typeclass.cast(writer))"""
          )
        )
    )
  }

  def postJobDtd(namespaceName: String, localName: String)(args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("dtd"), args)
    Term.Block(xmlWriter(q"writeDTD", Seq(argMap("dtd"))))
  }

  def postJobCdata(namespaceName: String, localName: String)(args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("cdata"), args)
    Term.Block(xmlWriter(q"writeCData", Seq(argMap("cdata"))))
  }

  def postJobComment(namespaceName: String, localName: String)(args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("cdata"), args)
    Term.Block(xmlWriter(q"writeComment", Seq(argMap("cdata"))))
  }

  def postJobProcInstr(namespaceName: String, localName: String)(args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("target", "data"), args)
    Term.Block(xmlWriter(q"writeProcessingInstruction",
        if (argMap contains "data")
          Seq(argMap("target"), argMap("data"))
        else
          Seq(argMap("target"))))
  }

  def postJobElement(namespaceName: String, localName: String)(args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq(), args)
    Term.Block(
        xmlWriter(q"writeStartElement", Seq[Term](Lit.String(localName))) ++
        argMap.foldLeft(List(
            q"""${toWorker(postJobContent(leftovers))}(c)"""
          , q"""typeclass.writeEndElement(typeclass.cast(writer))"""
        )){
          case (a, (k, v)) =>
            q"""typeclass.writeAttribute(typeclass.cast(writer),
$k.toString, $v.toString)""" :: a
        }
    )
  }
}
