package scala.ioc

import ioc._
import scala.collection.immutable.Seq
import scala.meta._
import scala.xml.stream.XmlWriterTypeclass

package object xml {
  def f[T](obj: Any) = obj.asInstanceOf[(Any, XmlWriterTypeclass[T])]

  private def xmlWriter(name: Term.Name, args: Seq[Term.Arg]) = {
    q"""
val (writer, typeclass) = scala.ioc.xml.f(c("xmlWriter"))
typeclass.$name(typeclass.cast(writer), ..$args)
"""
  }

  private def postJobContent(args: Seq[Term.Arg]): Term = {
    Term.Block(args.foldLeft(List[Term]())((acc, arg) => q"""
let("cdata", $arg,
  if (c("cdata") == ()) ()
  else ${xmlWriter(q"writeCharacters",
      Seq[Term](q"""c("cdata").toString"""))}
)
""" :: acc))
  }

  def postJobXml(namespaceName: String, localName: String)(args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) =
      mapArgs(Seq("version", "encoding"), args)
    q"""
${xmlWriter(q"writeStartDocument", Seq[Term](argMap("encoding"), argMap("version")))}
${toWorker(postJobContent(leftovers))}(c)
typeclass.writeEndDocument(typeclass.cast(writer))
"""
  }

  def postJobDtd(args: Seq[Term.Arg]): Tree = {
    val (argMap, leftovers, unspecified) = mapArgs(Seq("dtd"), args)
    xmlWriter(q"writeDTD", Seq(argMap("dtd")))
  }

//  def postJobComment(args: Seq[Seq[Term.Arg]]): Tree = {
//    (argMap, leftovers, unspecified) = mapArgs(Seq("cdata"), args)
//    q"""c("xmlWriter").writeComment(${argMap("cdata").asInstanceOf[Term]})"""
//  }
//
//  def postJobProcInstr(args: Seq[Seq[Term.Arg]]): Tree = {
//    (argMap, leftovers, unspecified) = mapArgs(Seq("target", "data"), args)
//    q"""c("xmlWriter").writeProcessingInstruction(${argMap("target").asInstanceOf[Term]}, ${argMap("data").asInstanceOf[Term]})"""
//  }
//
//  def postJobCdata(args: Seq[Seq[Term.Arg]]): Tree = {
//    (argMap, leftovers, unspecified) = mapArgs(Seq("cdata"), args)
//    q"""c("xmlWriter").writeCData(${argMap("cdata").asInstanceOf[Term]})"""
//  }
//
//  def postJobElement(args: Seq[Seq[Term.Arg]]): Tree = {
//    (argMap, leftovers, unspecified) = mapArgs(Seq("tagName", "attrs", "nodes"), args)
//    q"""c("xmlWriter").writeStartElement(${argMap("tagName").asInstanceOf[Term]})"""
//      ++ argMap.foldRight(Seq()) {
//        (attr, acc) => {
//          case q"($name, $value)" => acc :+ q"""c("xmlWriter").writeAttribute($name, $value)"""
//        }
//      }
//      :+ q"""c("xmlWriter").writeEndElement"""
//  }
}
