package scala.ioc.xml

import scala.ioc.ppm._
import scala.collection.immutable.ListSet
import scala.ppm._
import scala.reflect.runtime.universe
  import universe._
import scala.tools.reflect.ToolBox

package object ppm {

  private def metaWriteXml(name: String, args: List[Tree]) =
    q"""
((writer: javax.xml.stream.XMLStreamWriter) => {
  writer.${TermName(name)}(..$args)
})(`#scala.ioc#$$`("xmlWriter"))"""

  private def postJobContent(args: List[Tree]): Tree = {

    q"""..${

      args.foldLeft(List[Tree]())(

        (acc, arg) => q"""
((cdata: Any) =>
  if (() == cdata) ()
  else ${metaWriteXml("writeCharacters", List(q"""cdata.toString"""))}
)($arg)
""" :: acc
      )

    }"""
  }

  def postJobXml(
      namespaceName: Option[String],
      localName: String
    )(expr: Option[Tree],
      args: List[Tree], tb: ToolBox[universe.type], src: Option[String]
    ): Tree = {

    val enc = "enc"
    val ver = "ver"
    val dtd = "dtd"
    val ProcessedArgs(named, _, _, leftovers) =
      validateThisExprAndArgs(
        expr,
        args,
        allowExtraNamedArgs = true,
        allowExcessOrdinalArgs = true,
      )

    val unknownArgs = named.keySet diff ListSet(enc, ver, dtd)

    if (unknownArgs.size > 0)
      throw new Exception(s"unknown arguments: $unknownArgs")

    val contentsAndEndDocument = List(
      q"""..${postJobContent(leftovers.right.get)}""",
      metaWriteXml("writeEndDocument", List()),
    )

    q"""
..${
  metaWriteXml(
    "writeStartDocument",
    if (named.contains(ver))
      if (named.contains(enc))
            List(named(enc), named(ver))
      else
            List(named(ver))
    else if (named contains enc)
          List(named(enc), Literal(Constant("1.0")))
    else
      List()
  )::(
    if (named contains dtd)
      metaWriteXml("writeDTD", List(named(dtd)))::contentsAndEndDocument
    else
      contentsAndEndDocument
  )
}
"""

  }

  private def postJobCharacterData(methodName: String)
  (namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: List[Tree], tb: ToolBox[universe.type], src: Option[String]): Tree = {

    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
        expr,
        args,
        ListSet("cdata"),
      )

    metaWriteXml(methodName, List(named("cdata")))

  }

  def postJobCdata = postJobCharacterData("writeCData")_

  def postJobComment = postJobCharacterData("writeComment")_

  def postJobProcInstr(namespaceName: Option[String], localName: String)
  (expr: Option[Tree], args: List[Tree], tb: ToolBox[universe.type], src: Option[String]): Tree = {

    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
        expr,
        args,
        ListSet("target"),
        ListSet("data"),
      )

    metaWriteXml("writeProcessingInstruction",
      if (named contains "data")
        List(named("target"), named("data"))
      else
        List(named("target"))
    )

  }

  def postJobElement(namespaceName: Option[String] = None, localName: String)
  (expr: Option[Tree], args: List[Tree], tb: ToolBox[universe.type], src: Option[String]): Tree = {
    val exprIsPresent = expr != None
    val ProcessedArgs(named, _, _, leftovers) =
      validateThisExprAndArgs(
          expr,
          args,
          allowExtraNamedArgs = true,
          allowExcessOrdinalArgs = true,
        )

    q"""..${
      metaWriteXml("writeStartElement", List(Literal(Constant(localName))))::
      named.foldLeft(List(
          q"""..${postJobContent(leftovers.right.get)}""",
          metaWriteXml("writeEndElement", List()),
      )){
        case (acc, (name, value)) => metaWriteXml("writeAttribute", List(Literal(Constant(name)), value))::acc
      }
    }"""
  }

}
