package scala.ioc.json

import scala.ioc.ppm._
import scala.collection.immutable.ListSet
import scala.ppm._
import scala.reflect.runtime.universe
  import universe._
import scala.tools.reflect.ToolBox

package object ppm {

  private def metaWriteJson(name: String, args: List[Tree] = Nil) =
    q"""
((writer: com.fasterxml.jackson.core.JsonGenerator) => {
  writer.${TermName(name)}(..$args)
})(`#scalaioc#$$`("jsonWriter"))
"""

  private def postJobValue(treeVal: Tree): Tree = {

    q"""
$treeVal match {
  case _: com.fasterxml.jackson.core.JsonGenerator => ()
  case num: BigDecimal => ${metaWriteJson("write", List(q"""num"""))}
  case num: BigInteger => ${metaWriteJson("write", List(q"""num"""))}
  case bool: Boolean => ${metaWriteJson("write", List(q"""bool"""))}
  case num: Float => ${metaWriteJson("write", List(q"""num"""))}
  case num: Double => ${metaWriteJson("write", List(q"""num"""))}
  case num: Byte => ${metaWriteJson("write", List(q"""num"""))}
  case num: Short => ${metaWriteJson("write", List(q"""num"""))}
  case num: Int => ${metaWriteJson("write", List(q"""num"""))}
  case num: Long => ${metaWriteJson("write", List(q"""num"""))}
  case _: None => ${metaWriteJson("writeNull")}
  case _ => ${metaWriteJson("write", List(q"""cdata.toString"""))}
}
"""

  }

  def postJobJson(
      namespaceName: Option[String],
      localName: String
    )(macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
    val value = "value"
    val ProcessedArgs(named, _, _, _) =
      validateThisExprAndArgs(
        exprOpt,
        args,
        ListSet(value),
      )

    postJobValue(named(value))

  }

  def postJobObject(namespaceName: Option[String] = None, localName: String)
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
    val exprIsPresent = exprOpt != None
    val ProcessedArgs(named, _, _, _) =
      validateThisExprAndArgs(
          exprOpt,
          args,
          allowExtraNamedArgs = true,
        )

    q"""..${
      metaWriteJson("writeStartObject")::
      named.foldLeft(List(
          metaWriteJson("writeEndObject"),
      )){
        case (acc, (name, treeValue)) => metaWriteJson(
          "writeFieldName",
          List(Literal(Constant(name))),
        )::postJobValue(treeValue)::acc
      }
    }"""
  }

  def postJobArray(namespaceName: Option[String] = None, localName: String)
  (macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, targs, args, tb, src) = macroArgs
    val exprIsPresent = exprOpt != None
    val ProcessedArgs(_, _, _, leftovers) =
      validateThisExprAndArgs(
          exprOpt,
          args,
          allowExcessOrdinalArgs = true,
        )

    q"""..${
      metaWriteJson("writeStartArray")::(
        leftovers.getOrElse(throw new Exception("Programmatic error: flog the developer!")).foldLeft(
          List(metaWriteJson("writeEndArray"))
        ){
          case (acc, treeValue) => postJobValue(treeValue)::acc
        }
      )
    }"""

  }

}
