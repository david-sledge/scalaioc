package scala.ioc.json

import scala.collection.immutable.ListSet
import scala.ppm._
import scala.reflect.runtime.universe._

package object ppm {

  private def metaWriteJson(name: String, args: List[Tree] = Nil) =
    q"""
`#scalaioc#$$`[com.fasterxml.jackson.core.JsonGenerator]("jsonWriter").${TermName(name)}(..$args)
"""

  private def postJobValue(treeVal: Tree): Tree = {

    val valueTree = q"value"
    val argTrees = List(valueTree)
    q"""
scala.ioc.cast[Any]($treeVal) match {
  case value: java.math.BigDecimal => ${metaWriteJson("writeNumber", argTrees)}
  case value: java.math.BigInteger => ${metaWriteJson("writeNumber", argTrees)}
  case value: Boolean => ${metaWriteJson("writeBoolean", argTrees)}
  case value: Float => ${metaWriteJson("writeNumber", argTrees)}
  case value: Double => ${metaWriteJson("writeNumber", argTrees)}
  case value: Byte => ${metaWriteJson("writeNumber", argTrees)}
  case value: Short => ${metaWriteJson("writeNumber", argTrees)}
  case value: Int => ${metaWriteJson("writeNumber", argTrees)}
  case value: Long => ${metaWriteJson("writeNumber", argTrees)}
  case None => ${metaWriteJson("writeNull")}
  case () => ()
  case value => ${metaWriteJson("writeString", List(q"value.toString"))}
}
"""

  }

  def postJobJson(
      namespaceName: Option[String],
      localName: String
    )(macroArgs: MacroArgs): Tree = {

    val MacroArgs(exprOpt, _, args, tb, src) = macroArgs
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

    val MacroArgs(exprOpt, _, args, tb, src) = macroArgs
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

    val MacroArgs(exprOpt, _, args, _, _) = macroArgs
    val ProcessedArgs(_, _, _, leftovers) =
      validateThisExprAndArgs(
          exprOpt,
          args,
          allowExcessOrdinalArgs = true,
        )

    q"""..${
      metaWriteJson("writeStartArray"):: leftovers.getOrElse(throw new Exception("Programmatic error: flog the developer!")).foldLeft(
        List(metaWriteJson("writeEndArray"))
      ){
        case (acc, treeValue) => postJobValue(treeValue)::acc
      }
    }"""

  }

}
