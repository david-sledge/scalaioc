package sandbox

import org.scalatest._
import scala.meta._
import scala.ioc._

class Sandbox extends FlatSpec with Matchers {
  "This" should "just be an area for me to play around" in {
    println(q"null".structure)
    var factory = Factory()
    factory setLazyManager ("", c => "lazy")
    factory putToWork ""
  }
}
