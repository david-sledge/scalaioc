package scala.ioc

import org.scalatest._

class Spec extends FlatSpec with Matchers {
  def xml(version: String, encoding: String)(nodes: Any*): Unit = {}

  def dtd(dtd: String): Unit = {}

  def ^(nodes: Any*): Unit = {}

  def !(cdata: String): Unit = {}

  def ?(target: String, data: String): Unit = {}

  def cdata(cdata: String): Unit = {}

  def </>(tagName: String)(attrs: (String, String)*)(nodes: Any*): Unit = {}

//  def postJobXml(args: Seq[Seq[Term.Arg]]): Tree = {
//    val (argMap, leftovers, unspecified) =
//      mapArgs(Seq("version", "encoding", "nodes"), args)
//    q"""
//c("xmlWriter").writeStartDocument(
//  ${argMap("encoding")},
//  ${argMap("version")})"""
//      ++ postJobContent(Seq(argMap("nodes") match {
//        case q"""cn(..$nodes)""" => nodes
//        case node => Seq(node)
//      }))
//      ++ q"""
//val (xmlWriter, typeclass) = c("xmlWriter").asInstanceof[(Any, XmlWriterTypeclass[_])]
//typeclass.writeEndDocument(typeclass.cast(xmlWriter))
//"""
//    }
//  }

//  def postJobContent(args: Seq[Seq[Term.Arg]]): Tree = {
//    Term.Block(args.flatten.foldRight(Seq())(
//        (node, acc) => acc :+ q""")
//let("char",
//  $node,
//  if (c("char") == ()) () else c("xmlWriter").writeCharacters(c("char").toString))"""
//      )
//  }
//
//  def postJobDtd(args: Seq[Seq[Term.Arg]]): Tree = {
//    (argMap, leftovers, unspecified) = mapArgs(Seq("dtd"), args)
//    q"""c("xmlWriter").writeDTD(${argMap("dtd").asInstanceOf[Term]})"""
//  }
//
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
