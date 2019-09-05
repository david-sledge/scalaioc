package scala.ioc.cli

import scala.annotation.tailrec
import scala.ioc.ppm._
import scala.ioc.Factory

object Main extends App {

  private case class IocArgValues(val filenames: List[String] = List(), val workername: Option[String] = None)

  val prefix = "--I"

  private val IocArgFlags = Map(

      // function to extract staffing filenames
      ("f", (args: List[String], arg: String, iocArgValues: IocArgValues) => {

        args match {

          // make sure there's a follow up argument
          case (aarg::tail) => {

            (tail, IocArgValues(aarg::iocArgValues.filenames, iocArgValues.workername))

          }

          case _ => throw new Exception(s"No config file specified for '$prefix$arg'")

        }

      }),
    
      // function to extract initialization workername
      ("i", (args: List[String], arg: String, iocArgValues: IocArgValues) => {

        args match {

          // make sure there's a follow up argument
          case (aarg::tail) => {

            iocArgValues.workername match {

              // workername has already been specified
              case Some(name) =>
                throw new Exception(s"no more than one initialization worker may be specifed for '$prefix$arg'. Already specified as '$name'")

              case _ =>
                (tail, IocArgValues(iocArgValues.filenames, Some(aarg)))
            }

          }

          case _ => throw new Exception(s"No workername specified for '$prefix$arg'")

        }

      }),
  )

  def getIocArgs = IocArgFlags.keySet

  /*
   * separate the IoC arguments from the rest of the arguments
   */
  @tailrec
  private def extractIocArgs(
      args: List[String],
      splitArgs: (IocArgValues, List[String]),
    ): (IocArgValues, List[String]) = {

    args match {

      case (arg::tail) => {

        val (extracted, filtered) = splitArgs

        // IoC argument?
        if (arg.startsWith(prefix)) {

          val a = arg.substring(prefix.length)

          val (ttail, iocArgValues) = IocArgFlags.get(a) match {

            case Some(f) => f(tail, a, extracted)

            case _ => throw new Exception(s"Unknown IoC arg flag: $arg")

          }

          extractIocArgs(ttail, (iocArgValues, filtered))

        }
        else {

          // not an IoC arg; add it to the filted args to be passed to the underlying app
          extractIocArgs(tail, (extracted, arg::filtered))

        }

      }

      // no more args; return the results
      case _ => splitArgs

    }

  }

  private val (iocArgValues, filtered) = extractIocArgs(args.toList, (IocArgValues(), List[String]()))

  // build the factory and staff it
  val factory = (Factory() /: (

      if (iocArgValues.filenames == Nil) {
        // default filename
        List("sfaff.fsp")
      }
      else {
        iocArgValues.filenames
      }
    ))(
      (acc, filename) => {
        val (factory, _) = staffFactoryFromFile(fileName = filename, factory = acc)
        factory
      }
    )

  val obj = factory.putToWork(
      iocArgValues.workername match {

        case Some(name) => name

        // default initialization workername
        case _ => "init"
      },
      Map("args" -> filtered.reverse.toArray),
  )

}
