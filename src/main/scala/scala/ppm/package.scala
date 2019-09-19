package scala

package object ppm {

  import scala.collection.immutable.Map
  import scala.collection.immutable.ListSet
  import scala.reflect.runtime.universe
    import universe._
  import scala.tools.reflect.ToolBox

  type Macro = (
      Option[Tree],
      List[Tree],
      ToolBox[universe.type],
      Option[String],
    ) => Tree

  type NamespacedMacro = (Option[String], String) => Macro

  final case class ProcessedArgs(
    named: Map[String, Tree],
    duplicates: Map[String, List[Tree]],
    missingArguments: Set[String],
    leftovers: Either[ListSet[String], List[Tree]],
  )

  /**
   * Takes the given arguments and assigns them to the given argument names
   * using the following rules:
   *
   *   - named arguments are handled first
   *   - ordinal arguments will be assigned to the remaining variable names in
   *   the order that they appear where the required arguments are listed before
   *   the optional ones.
   *
   * @author sledge
   * @param args the arguments passed into the macro
   * @param requiredArgNames required arguments of the macro
   * @param optionalArgNames optional arguments of the macro
   * @return ProcessedArgs object
   */
  def processArgs(
      args: List[Tree],
      requiredArgNames: ListSet[String] = ListSet(),
      optionalArgNames: ListSet[String] = ListSet(),
    ) = {
    val argNames = requiredArgNames ++ optionalArgNames
    // address the named arguments first, then handle the ordinal arguments
    val (named, duplicates, ordinal, ordNames) = args.foldRight(
        (Map[String, Tree](), Map[String, List[Tree]](), List[Tree](), argNames)
      )(
        (t, acc) => {
          val (map, dups, list, ordArgNames) = acc

          t match {
            // named argument
            case AssignOrNamedArg(Ident(TermName(name)), expr) => {
              if (map contains name) {
                (
                  // remove it from the map
                  map - name,
                  // add it to the dups
                  dups + (name -> List(expr, map(name))),
                  list,
                  ordArgNames - name
                )
              }
              else if (dups contains name) {
                (
                  map,
                  // add it to the dups
                  dups + (name -> (dups(name) :+ expr)),
                  list,
                  ordArgNames - name
                )
              }
              else
                (
                  // add it to the name/arg map
                  map + (name -> expr),
                  dups,
                  list,
                  ordArgNames - name
                )
            }

            // unnamed argument
            case _ => (
                map,
                dups,
                // add it to the list of ordinal arguments to be processed later
                t :: list,
                ordArgNames
              )
          }
        }
      )

    // fill the gaps with the ordinal arguments
    val (named0, ordinal0, ordNames0) = ordinal.foldLeft((named, List[Tree](), ordNames))(
        (acc, t) => {
          val (map, ord, ordArgNames) = acc
          ordArgNames.headOption match {
            // add it to the name/arg map
            case Some(name) => (map + (name -> t), ord, ordArgNames.tail)
            // no more names; add it to the ordinal overflow list
            case _ => (map, ord :+ t, ordArgNames)
          }
        }
      )

    ProcessedArgs(
      // arguments mapped to the supplied argument names
      named0,
      duplicates,
      // arguments that were passed by name, but were not among the list of required or optional names
      named0.foldRight(Set[String]())((entry, acc) => {
        val key = entry._1
        if (argNames contains key) acc else acc + key
      }),
      {
        // don't care about the optional argument names because they're... optional
        val unusedRequiredArgNames = ordNames0 diff optionalArgNames

        if (unusedRequiredArgNames.size > 0) {
          // required arguments that were not supplied
          Left(unusedRequiredArgNames)
        }
        else {
          // possibly more ordinal arguments than available argument names
          Right(ordinal0.reverse)
        }
      },
    )
  }

  sealed class RequiredOrExcessArgError

  case object NoError extends RequiredOrExcessArgError
  case object ExcessiveOrdinalArgs extends RequiredOrExcessArgError
  case object MissingRequiredArgs extends RequiredOrExcessArgError

  val buildErrorMessage = (
      named: Map[String, Tree],
      duplicates: Map[String, List[Tree]],
      extraNames: Set[String],
      leftovers: Either[ListSet[String], List[Tree]],
      hasUnsupportedArgNames: Boolean,
      hasDuplicateArgNames: Boolean,
      requiredOrExcessArgError: RequiredOrExcessArgError,
    ) => {
    val errorMsgs = if (hasDuplicateArgNames)
        List(s"has duplicate named arguments: ${
          duplicates.map{case (key, value) => s"$key (${value.size})"}.mkString(", ")
        }")
      else
        List()
    val errorMsgs1 = requiredOrExcessArgError match {
        case MissingRequiredArgs => s"is missing arguments: ${
            leftovers.left.get
          }"::errorMsgs
        case ExcessiveOrdinalArgs => s"has ${
          leftovers.right.get.size
        } too many arguments"::errorMsgs
        case _ => errorMsgs
      }
    val errorMsgs2 = if (hasUnsupportedArgNames)
        s" has unsupported named arguments: ${
          extraNames
        }"::errorMsgs1
    else errorMsgs1

    if (errorMsgs2.size > 0)
      s"""macro call ${errorMsgs2.mkString("; ")}"""
    else
      ""

  }

  def validateArgs(
      args: List[Tree],
      requiredArgNames: ListSet[String] = ListSet(),
      optionalArgNames: ListSet[String] = ListSet(),
      allowExtraNamedArgs: Boolean = false,
      allowDuplicateArgs: Boolean = false,
      allowExcessOrdinalArgs: Boolean = false,
      processArgErrors: (
          Map[String, Tree],
          Map[String, List[Tree]],
          Set[String],
          Either[ListSet[String], List[Tree]],
          Boolean,
          Boolean,
          RequiredOrExcessArgError,
        ) =>
        ProcessedArgs = (a, b, c, d, e, f, g) => {
          throw new Exception(buildErrorMessage(a, b, c, d, e, f, g))
        },
    ) = {
    val processedArgs@ProcessedArgs(named, duplicates, extraNames, leftovers) =
      processArgs(
        args,
        requiredArgNames,
        optionalArgNames,
      )

    val requiredOrExcessArgError = 
      if (leftovers.isLeft)
        MissingRequiredArgs
      else if (leftovers.right.get.size > 0 && !allowExcessOrdinalArgs)
        ExcessiveOrdinalArgs
      else
        NoError

    lazy val extraNamedArgError = !allowExtraNamedArgs && extraNames.size > 0
    lazy val duplicateArgError = !allowDuplicateArgs && duplicates.size > 0

    if (requiredOrExcessArgError != NoError ||
        extraNamedArgError ||
        duplicateArgError) {
      processArgErrors(
          named,
          duplicates,
          extraNames,
          leftovers,
          extraNamedArgError,
          duplicateArgError,
          requiredOrExcessArgError,
        )
    }
    else
      processedArgs
  }

  sealed abstract class ThisExprPresence

  case object Optional extends ThisExprPresence
  case object Required extends ThisExprPresence
  case object Forbidden extends ThisExprPresence

  val unsupportedExprErrorMessage = "macro does not use a method-containing object;"
  val missingExprErrorMessage = "macro requires a method-containing object;"

  def validateThisExprAndArgs(
      thisExpr: Option[Tree],
      args: List[Tree],
      requiredArgNames: ListSet[String] = ListSet(),
      optionalArgNames: ListSet[String] = ListSet(),
      thisExprPresence: ThisExprPresence = Forbidden,
      allowExtraNamedArgs: Boolean = false,
      allowDuplicateArgs: Boolean = false,
      allowExcessOrdinalArgs: Boolean = false,
      processErrors: (
          Option[Tree],
          Map[String, Tree],
          Map[String, List[Tree]],
          Set[String],
          Either[ListSet[String], List[Tree]],
          Boolean,
          Boolean,
          Boolean,
          RequiredOrExcessArgError,
        ) =>
        ProcessedArgs = (thisExpr, a, b, c, d, hasThisExprError, e, f, g) => {

          throw new Exception(s"${
            if (hasThisExprError) s"${
              if (thisExpr == None)
                missingExprErrorMessage
              else
                unsupportedExprErrorMessage
            } "
          else
            ""
          }${buildErrorMessage(a, b, c, d, e, f, g)}")

        },
    ) = {

    lazy val exprIsMissing = thisExpr == None
    lazy val hasThisExprError = thisExprPresence match {
        case Forbidden => !exprIsMissing
        case Required => exprIsMissing
        case _ => false
      }

    val processedArgs@ProcessedArgs(named, duplicates, extraArgNames, leftovers) = validateArgs(
        args,
        requiredArgNames,
        optionalArgNames,
        allowExtraNamedArgs,
        allowDuplicateArgs,
        allowExcessOrdinalArgs,
        (a, b, c, d, e, f, g) => {
          processErrors(thisExpr, a, b, c, d, hasThisExprError, e, f, g)
        },
      )

    if (hasThisExprError) processErrors(
        thisExpr,
        named,
        duplicates,
        extraArgNames,
        leftovers,
        hasThisExprError,
        false,
        false,
        NoError,
      )
    else
      processedArgs

  }

}
