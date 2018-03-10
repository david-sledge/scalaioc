package scala.ioc

import org.scalatest._
import meta._
import scala.collection.immutable.Seq

class IocSpec extends FlatSpec with Matchers {

  "Mapping arguments" should "use all the argument names or all the arguments or both" in {
    val varNames = Seq("id", "name", "key")
    val args = Seq(
        q"cheese",
        q"Map[Any, Any]()"
        )
    val (named, ordinal, unused) = mapArgs(varNames, args)
    ordinal.size shouldBe 0
    unused.size shouldBe 1
    unused(0) shouldBe "key"
  }
}
