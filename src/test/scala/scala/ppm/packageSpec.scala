package scala.ppm

import scala.collection.immutable.ListSet
import scala.reflect.runtime.universe._

import org.scalatest._

class packageSpec extends FlatSpec with Matchers {

  "processArgs" should "divvy up the arguments and assign them to the supplied parameters" in {

    val Apply(expr, args) = q"a((), c = true, e = 0, c = false)"

    val ProcessedArgs(named, duplicates, extraNames, leftovers) = processArgs(
      args,
      ListSet("a", "b"),
      ListSet("c", "d"),
    )

    named.map(entry => (entry._1, entry._2.toString)) shouldBe Map(
        "e" -> q"0".toString,
        "a" -> q"()".toString,
        )
    duplicates.map(entry => (entry._1, entry._2.map(_.toString))) shouldBe Map(
        "c" -> List(q"true".toString, q"false".toString),
        )
    extraNames shouldBe Set("e")
    leftovers shouldBe Left(ListSet("b"))

    val Apply(expr2, args2) = q"a((), c = true, e = 0, None, Left(false), string)"

    val ProcessedArgs(named2, duplicates2, extraNames2, leftovers2) = processArgs(
      args2,
      ListSet("a", "b"),
      ListSet("c", "d"),
    )

    named2.map(entry => (entry._1, entry._2.toString)) shouldBe Map(
        "c" -> q"true".toString,
        "e" -> q"0".toString,
        "a" -> q"()".toString,
        "b" -> q"None".toString,
        "d" -> q"Left(false)".toString,
        )
    duplicates2.map(entry => (entry._1, entry._2.map(_.toString))) shouldBe Map()
    extraNames2 shouldBe Set("e")
    leftovers2.isRight shouldBe true
    leftovers2.map(_.map(_.toString)) shouldBe Right(List(q"string".toString))

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

    val ProcessedArgs(named2, duplicates2, extraNames2, leftovers2) = validateArgs(
      args2,
      ListSet("a", "b"),
      ListSet("c", "d"),
    )

    named2.map(entry => (entry._1, entry._2.toString)) shouldBe Map(
      "c" -> q"true".toString,
      "a" -> q"()".toString,
      "b" -> q"string".toString,
    )
    duplicates2.map(entry => (entry._1, entry._2.map(_.toString))) shouldBe Map()
    extraNames2 shouldBe Set()
    leftovers2.map(_.map(_.toString)) shouldBe Right(List())

  }

  it should "not allow extraneous arguments by default" in {

    val Apply(expr, args) = q"a(true, e = None, false, 0, 1, string)"

    assertThrows[Exception] {
        validateArgs(
        args,
        ListSet("a", "b"),
        ListSet("c", "d"),
      )
    }

  }

  it should "allow extraneous named arguments when specified" in {

    val Apply(expr, args) = q"a(true, e = None, false, 0, 1)"

    val ProcessedArgs(named, duplicates, extraNames, leftovers) = validateArgs(
      args,
      ListSet("a", "b"),
      ListSet("c", "d"),
      true,
    )

    named.map(entry => (entry._1, entry._2.toString)) shouldBe Map(
      "e" -> q"None".toString,
      "a" -> q"true".toString,
      "b" -> q"false".toString,
      "c" -> q"0".toString,
      "d" -> q"1".toString,
    )
    duplicates.map(entry => (entry._1, entry._2.map(_.toString))) shouldBe Map()
    extraNames shouldBe Set("e")
    leftovers shouldBe Right(List())

  }

  it should "allow extraneous ordinal arguments when specified" in {

    val Apply(expr, args) = q"a(true, None, false, 0, 1)"

    val ProcessedArgs(named, duplicates, extraNames, leftovers) = validateArgs(
      args,
      ListSet("a", "b"),
      ListSet("c", "d"),
      allowExcessOrdinalArgs = true,
    )

    named.map(entry => (entry._1, entry._2.toString)) shouldBe Map(
      "a" -> q"true".toString,
      "b" -> q"None".toString,
      "c" -> q"false".toString,
      "d" -> q"0".toString,
    )
    duplicates.map(entry => (entry._1, entry._2.map(_.toString))) shouldBe Map()
    extraNames shouldBe Set()
    leftovers.map(_.map(_.toString)) shouldBe Right(List(q"1".toString))

  }

  it should "not allow duplicate arguments by default" in {

    val Apply(expr, args) = q"a(e = true, e = None, false, 0, 1, string)"

    assertThrows[Exception] {
        validateArgs(
        args,
        ListSet("a", "b"),
        ListSet("c", "d"),
      )
    }

  }

  it should "allow duplicate arguments when specified" in {

    val Apply(expr, args) = q"a(c = true, c = None, false, 0, 1)"

    val ProcessedArgs(named, duplicates, extraNames, leftovers) = validateArgs(
      args,
      ListSet("a", "b"),
      ListSet("c", "d"),
      allowDuplicateArgs = true,
    )

    named.map(entry => (entry._1, entry._2.toString)) shouldBe Map(
      "a" -> q"false".toString,
      "b" -> q"0".toString,
      "d" -> q"1".toString,
    )
    duplicates.map(entry => (entry._1, entry._2.map(_.toString))) shouldBe Map("c" -> List(q"true".toString, q"None".toString))
    extraNames shouldBe Set()
    leftovers.map(_.map(_.toString)) shouldBe Right(List())

  }

}
