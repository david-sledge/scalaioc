package scala.ioc.xml

import scala.collection.immutable.ListSet
import scala.ppm._
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

package object ppm {

  private def metaWriteXml(name: String, args: List[Tree]) =
    q"""
`#scalaioc#$$`[javax.xml.stream.XMLStreamWriter]("xmlWriter").${TermName(name)}(..$args)
"""

  private def postJobContent(args: List[Tree]): Tree = {

    q"""..${

      args.foldLeft(List.empty[Tree])(

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
    )(macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, _, args, _, _) = macroArgs
    val enc = "enc"
    val ver = "ver"
    val dtd = "dtd"
    val ProcessedArgs(named, _, _, leftovers) =
      validateThisExprAndArgs(
        exprOpt,
        args,
        allowExtraNamedArgs = true,
        allowExcessOrdinalArgs = true,
      )

    val unknownArgs = named.keySet diff ListSet(enc, ver, dtd)

    if (unknownArgs.nonEmpty)
      throw new Exception(s"unknown arguments: '${unknownArgs.mkString("', '")}'")

    val contentsAndEndDocument = List(
      q"""..${postJobContent(leftovers.getOrElse(throw new Exception("Programmatic error: flog the developer!")))}""",
      metaWriteXml("writeEndDocument", Nil),
    )

    lazy val containsVersion = named contains ver
    lazy val version =
      if (containsVersion)
        named(ver)
      else
        Literal(Constant("1.0"))

    q"""
..${
      (
        if (named contains enc)
          metaWriteXml("writeStartDocument", List(named(enc), version))
        else
          q"""
if (c contains "enc")
  ${metaWriteXml("writeStartDocument", List(q"""`#scalaioc#$$`("enc")""", version))}
else
  ${
    metaWriteXml("writeStartDocument",
      if (containsVersion)
        List(named(ver))
      else
        Nil
    )
  }
"""
      )::
      (
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
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
        exprOpt,
        args,
        ListSet("cdata"),
      )

    metaWriteXml(methodName, List(named("cdata")))

  }

  def postJobCdata: (Option[String], String) => MacroArgs => universe.Tree = postJobCharacterData("writeCData")

  def postJobComment: (Option[String], String) => MacroArgs => universe.Tree = postJobCharacterData("writeComment")

  def postJobProcInstr(namespaceName: Option[String], localName: String)
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
    val ProcessedArgs(named, _, _, _) = validateThisExprAndArgs(
        exprOpt,
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
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, _, args, _, _) = macroArgs
    val ProcessedArgs(named, _, _, leftovers) =
      validateThisExprAndArgs(
          exprOpt,
          args,
          allowExtraNamedArgs = true,
          allowExcessOrdinalArgs = true,
        )

    q"""..${
      metaWriteXml("writeStartElement", List(Literal(Constant(localName))))::
      named.foldLeft(List(
          q"""..${postJobContent(leftovers.getOrElse(throw new Exception("Programmatic error: flog the developer!")))}""",
          metaWriteXml("writeEndElement", Nil),
      )){
        case (acc, (name, value)) => metaWriteXml("writeAttribute", List(Literal(Constant(name)), value))::acc
      }
    }"""
  }

}
