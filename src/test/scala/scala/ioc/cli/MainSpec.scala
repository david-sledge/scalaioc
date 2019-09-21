package scala.ioc.cli

import org.scalatest._

class MainSpec extends FlatSpec with Matchers {

  "CLI entry point" should
  "consume flags prefixed with '--I' (and associated arguments) and pass the rest to the init worker" in {

    Main.main(Array(
      "A", "--If", "src/test/resources/tempAgency.fsp", "b", "--Ii", "letsGetStarted", "C",
    ))

    MainSpec.args should not be (None)
    MainSpec.args.get.deep should be (Array("A", "b", "C").deep)
  }

  it should "default to 'staff.fsp' when '--If' is not supplied" in {

    Main.main(Array(
      "A", "b", "--Ii", "letsGetStarted", "C",
    ))

  }

  it should "default to 'init' when '--Ii' is not supplied" in {

    Main.main(Array(
      "A", "--If", "src/test/resources/tempAgency.fsp", "b", "C",
    ))

  }

}

object MainSpec {

  var args: Option[Array[String]] = None

  def processArgs(args: Array[String]) = {
    this.args = Some(args)
  }

}
