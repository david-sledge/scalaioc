package scala.ppm

import scala.reflect.runtime.universe._

final case class MacroNotFoundException(
    ns: Option[String],
    localName: String,
    src: Option[String],
    pos: Position,
    )
  extends Exception(s""""$localName"${
    ns match {
            case Some(name) => s""" in namespace "$name""""
            case _ => ""
          }
  } ($src:$pos)""")

final case class UnboundReferrerException(ref: String, src: Option[String], pos: Position)
  extends Exception(s""""$ref" ($src:$pos)""")

final case class MacroException(
    e: Exception,
    ns: Option[String],
    localName: String,
    src: Option[String],
    pos: Position,
    )
  extends Exception(s"$e ($src:$pos)")
