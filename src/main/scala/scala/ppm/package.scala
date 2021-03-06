package scala

import scala.reflect.runtime.universe

package object ppm {

  import scala.collection.immutable.ListMap
  import scala.collection.immutable.ListSet
  import scala.io.Source._
  import scala.reflect.runtime.universe
    import universe._
  import scala.tools.reflect.ToolBox

  final case class MacroArgs(
    thisExpr: Option[Tree],
    targs: List[Tree],
    args: List[Tree],
    tb: ToolBox[universe.type],
    src: Option[String],
  )

  type Macro = MacroArgs => Tree

  type NamespacedMacro = (Option[String], String) => Macro

  final case class ProcessedArgs(
    named: ListMap[String, Tree],
    duplicates: ListMap[String, List[Tree]],
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
      requiredArgNames: ListSet[String] = ListSet.empty,
      optionalArgNames: ListSet[String] = ListSet.empty,
    ): ProcessedArgs = {
    val argNames = requiredArgNames ++ optionalArgNames
    // address the named arguments first, then handle the ordinal arguments
    val (named, duplicates, ordinal, ordNames) = args.foldRight(
        (ListMap.empty[String, Tree], ListMap.empty[String, List[Tree]], List.empty[Tree], argNames)
      )(
        (t, acc) => {
          val (map, dups, list, ordArgNames) = acc

          t match {
            // named argument
            case NamedArg(Ident(TermName(name)), expr) =>
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
    val (named0, ordinal0, ordNames0) = ordinal.foldLeft((named, List.empty[Tree], ordNames))(
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
      (duplicates.keySet ++ named0.keySet).foldRight(Set.empty[String])(
        (key, acc) => if (argNames contains key) acc else acc + key
      ),
      {
        // don't care about the optional argument names because they're... optional
        val unusedRequiredArgNames = ordNames0 diff optionalArgNames

        if (unusedRequiredArgNames.nonEmpty) {
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

  val buildErrorMessage: (ListMap[String, universe.Tree], ListMap[String, List[universe.Tree]], Set[String], Either[ListSet[String], List[universe.Tree]], Boolean, Boolean, RequiredOrExcessArgError) => String = (
      named: ListMap[String, Tree],
      duplicates: ListMap[String, List[Tree]],
      extraNames: Set[String],
      leftovers: Either[ListSet[String], List[Tree]],
      hasUnsupportedArgNames: Boolean,
      hasDuplicateArgNames: Boolean,
      requiredOrExcessArgError: RequiredOrExcessArgError,
    ) => {
    val errorMsgs = if (hasDuplicateArgNames)
        List(s"has duplicate named arguments: ${
          duplicates.map{case (key, value) => s"'$key' (${value.size})"}.mkString(", ")
        }")
      else
        Nil
    val errorMsgs1 = requiredOrExcessArgError match {
        case MissingRequiredArgs => s"is missing arguments: '${
            leftovers.swap.getOrElse(throw new Exception("Programmatic error: flog the developer!")).mkString("', '")
          }'"::errorMsgs
        case ExcessiveOrdinalArgs => s"has ${
          leftovers.getOrElse(throw new Exception("Programmatic error: flog the developer!")).size
        } too many arguments"::errorMsgs
        case _ => errorMsgs
      }
    val errorMsgs2 = if (hasUnsupportedArgNames)
        s" has unsupported named arguments: '${
          extraNames.mkString("', '")
        }'"::errorMsgs1
    else errorMsgs1

    if (errorMsgs2.nonEmpty)
      s"""macro call ${errorMsgs2.mkString("; ")}"""
    else
      ""

  }

  def validateArgs[T](
      args: List[Tree],
      requiredArgNames: ListSet[String] = ListSet(),
      optionalArgNames: ListSet[String] = ListSet(),
      allowExtraNamedArgs: Boolean = false,
      allowDuplicateArgs: Boolean = false,
      allowExcessOrdinalArgs: Boolean = false,
      onValid: ProcessedArgs => T = (a: ProcessedArgs) => a : ProcessedArgs,
      onInvalid: (
          ListMap[String, Tree],
          ListMap[String, List[Tree]],
          Set[String],
          Either[ListSet[String], List[Tree]],
          Boolean,
          Boolean,
          RequiredOrExcessArgError,
        ) => T =
          (
            a: ListMap[String, Tree],
            b: ListMap[String, List[Tree]],
            c: Set[String],
            d: Either[ListSet[String], List[Tree]],
            e: Boolean,
            f: Boolean,
            g: RequiredOrExcessArgError,
          ) => throw new Exception(buildErrorMessage(a, b, c, d, e, f, g)),
    ): Any = {

    val processedArgs@ProcessedArgs(named, duplicates, extraNames, leftovers) =
      processArgs(
        args,
        requiredArgNames,
        optionalArgNames,
      )

    val requiredOrExcessArgError = 
      if (leftovers.isLeft)
        MissingRequiredArgs
      else if (leftovers.getOrElse(throw new Exception("Programmatic error: flog the developer!")).nonEmpty && !allowExcessOrdinalArgs)
        ExcessiveOrdinalArgs
      else
        NoError

    lazy val extraNamedArgError = !allowExtraNamedArgs && extraNames.nonEmpty
    lazy val duplicateArgError = !allowDuplicateArgs && duplicates.nonEmpty

    if (requiredOrExcessArgError != NoError ||
        extraNamedArgError ||
        duplicateArgError) {
      onInvalid(
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

  def validateThisExprAndArgs[T](
      thisExpr: Option[Tree],
      args: List[Tree],
      requiredArgNames: ListSet[String] = ListSet(),
      optionalArgNames: ListSet[String] = ListSet(),
      thisExprPresence: ThisExprPresence = Forbidden,
      allowExtraNamedArgs: Boolean = false,
      allowDuplicateArgs: Boolean = false,
      allowExcessOrdinalArgs: Boolean = false,
      onValid: ProcessedArgs => T = (a: ProcessedArgs) => a : ProcessedArgs,
      onInvalid: (
          Option[Tree],
          ListMap[String, Tree],
          ListMap[String, List[Tree]],
          Set[String],
          Either[ListSet[String], List[Tree]],
          Boolean,
          Boolean,
          Boolean,
          RequiredOrExcessArgError,
        ) =>
        T =
          (
            thisExpr: Option[Tree],
            a: ListMap[String, Tree],
            b: ListMap[String, List[Tree]],
            c: Set[String],
            d: Either[ListSet[String], List[Tree]],
            hasThisExprError: Boolean,
            e: Boolean,
            f: Boolean,
            g: RequiredOrExcessArgError,
          ) => {

          throw new Exception(s"${
            if (hasThisExprError) s"${
              if (thisExpr.isEmpty)
                missingExprErrorMessage
              else
                unsupportedExprErrorMessage
            } "
          else
            ""
          }${buildErrorMessage(a, b, c, d, e, f, g)}")

        },
    ): Any = {

    lazy val exprIsMissing = thisExpr.isEmpty
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
        onValid,
        (
          a: ListMap[String, Tree],
          b: ListMap[String, List[Tree]],
          c: Set[String],
          d: Either[ListSet[String], List[Tree]],
          e: Boolean,
          f: Boolean,
          g: RequiredOrExcessArgError,
        ) => {
          onInvalid(thisExpr, a, b, c, d, hasThisExprError, e, f, g)
        },
      )

    if (hasThisExprError) onInvalid(
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

  def execute[T](
      code: String,
      preprocessor: Preprocessor,
      src: Option[String] = None,
    ): T = {
    // obtain toolbox
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox(options = "")
    // generate the AST
    val tree = tb.parse(code)
    //println(s"Position is: ${tree.pos}")
    // manipulate tree
    val transformedTree = preprocessor.transformTree(tree, tb, src)
    //println(transformedTree)
    // compile
    val f = tb.compile(transformedTree)
    // ... execute
    f().asInstanceOf[T]
  }

  // TODO:  scaladoc
  def executeFromFile[T](fileName: String, encoding: String = "utf-8", preprocessor: Preprocessor): Nothing =
  // TODO: close source
    execute(fromFile(fileName, encoding).mkString, preprocessor, Some(fileName))

  // TODO:  scaladoc
  def executeFromResource[T](path: String, encoding: String = "utf-8", preprocessor: Preprocessor): Nothing =
    executeFromStream(getClass.getClassLoader.getResourceAsStream(path), encoding, preprocessor, Some(path))

  // TODO:  scaladoc
  def executeFromStream[T](stream: java.io.InputStream, encoding: String = "utf-8", preprocessor: Preprocessor, src: Option[String] = None): Nothing =
    execute(scala.io.Source.fromInputStream(stream, encoding).mkString, preprocessor, src)

}
