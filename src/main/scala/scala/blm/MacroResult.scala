package scala.blm

import scala.reflect.runtime.universe._

final case class MacroNotFoundException(ns: Option[String], localName: String, src: Option[String], pos: Position)
  extends Exception("\"" + localName + "\""
      + (
          ns match {
            case Some(name) => " in namespace \"" + name + "\""
            case _ => ""
          }
        ) + " (" + src + ":" + pos + ")")
final case class UnboundReferrerException(ref: String, src: Option[String], pos: Position)
  extends Exception("\"" + ref + "\" (" + src + ":" + pos + ")")
final case class MissingRequiredObjectOrArguments(ns: Option[String], localName: String, isMissingRequiredObj: Boolean, missingRequiredArgs: Seq[String], src: Option[String], pos: Position)
  extends Exception("\"" + localName + "\""
      + (
          ns match {
            case Some(name) => " in namespace \"" + name + "\""
            case _ => ""
          }
        ) + " is" + (if (isMissingRequiredObj) " missing method-containing object." else "") + " missing arguments:  " + missingRequiredArgs + " (" + src + ":" + pos + ")")
final case class MacroException(e: Exception, src: Option[String], pos: Position)
  extends Exception(e + " (" + src + ":" + pos + ")")

sealed abstract class MacroResult[T]

final case class MacroNotFound[T](ns: Option[String], localName: String)
  extends MacroResult[T]
final case class UnboundReferrer[T](ref: String) extends MacroResult[T]
final case class Error[T](ns: Option[String], localName: String, isMissingRequiredObj: Boolean, missingRequiredArgs: Seq[String]) extends MacroResult[T]
final case class Ok[T](result: T) extends MacroResult[T]
