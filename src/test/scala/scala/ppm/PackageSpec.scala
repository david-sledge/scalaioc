package scala.ppm

import scala.collection.immutable.ListSet
import scala.reflect.runtime.universe._

import org.scalatest._

class PackageSpec extends FlatSpec with Matchers {

  "processArgs" should "divvy up the arguments and assign them to the supplied parameters" in {

    val Apply(expr, args) = q"a((), c = true, e = 0, c = false)"

    val ProcessedArgs(named, duplicates, extraNames, leftovers) = processArgs(
      args,
      ListSet("a", "b"),
      ListSet("c", "d"),
    )

    println(named)
    named.size should be (2)
    duplicates.size should be (1)
    extraNames should be (Set("e"))
    leftovers should be (Left(ListSet("b")))

    val Apply(expr2, args2) = q"a((), c = true, e = 0, None, Left(false), string)"

    val ProcessedArgs(named2, duplicates2, extraNames2, leftovers2) = processArgs(
      args2,
      ListSet("a", "b"),
      ListSet("c", "d"),
    )

    named2.size should be (5)
    duplicates2.size should be (0)
    extraNames2 should be (Set("e"))
    leftovers2.isRight should be (true)
    leftovers2.right.get.size should be (1)

  }

  "validateArgs" should "verify that the all the required arguments have been supplied" in {

    val Apply(expr, args) = q"a(true)"

    assertThrows[Exception] {
        validateArgs(
        args,
        ListSet("a", "b"),
        ListSet("c", "d"),
      )
    }

    val Apply(expr2, args2) = q"a((), c = true, string)"

    validateArgs(
      args2,
      ListSet("a", "b"),
      ListSet("c", "d"),
    )

  }

  "validateArgs" should "not allow extraneous arguments by default" in {

    val Apply(expr, args) = q"a(true, e = None, false, 0, 1, string)"

    assertThrows[Exception] {
        validateArgs(
        args,
        ListSet("a", "b"),
        ListSet("c", "d"),
      )
    }

  }

  "validateArgs" should "allow extraneous named arguments when specified" in {

    val Apply(expr, args) = q"a(true, e = None, false, 0, 1)"

    validateArgs(
      args,
      ListSet("a", "b"),
      ListSet("c", "d"),
      true,
    )

  }

  "validateArgs" should "allow extraneous ordinal arguments when specified" in {

    val Apply(expr, args) = q"a(true, None, false, 0, 1)"

    validateArgs(
      args,
      ListSet("a", "b"),
      ListSet("c", "d"),
      allowExcessOrdinalArgs = true,
    )

  }

  "validateArgs" should "not allow duplicate arguments by default" in {

    val Apply(expr, args) = q"a(e = true, e = None, false, 0, 1, string)"

    assertThrows[Exception] {
        validateArgs(
        args,
        ListSet("a", "b"),
        ListSet("c", "d"),
      )
    }

  }

  "validateArgs" should "allow duplicate arguments when specified" in {

    val Apply(expr, args) = q"a(e = true, e = None, false, 0, 1, string)"

    validateArgs(
      args,
      ListSet("a", "b"),
      ListSet("c", "d"),
      allowDuplicateArgs = true,
    )

  }

}
