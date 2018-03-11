package scala.ioc

import scala.ioc.xml._
import org.scalatest._
import scala.meta._

class StaffingSpec extends FlatSpec with Matchers {
  "The IoC transformer" should "manipulate the scala AST" in {
    val staffing = Staffing()
    staffing.transformIoC(q"""`#scala.ioc#singleton`("id", "value")""").structure should be
      q"""factory.setLazyManager("id", (c: Map[Any, Any]) => "value")""".structure
  }

  "A staffing object" should "hire recruiters" in {
    val staffing = new Staffing()
    staffing.addRecruiter("scala.xml", "xml", postJobXml)
    staffing.addRecruiter("scala.xml", "dtd", postJobDtd)
    staffing.addRecruiter("scala.xml", "cdata", postJobCdata)
    staffing.addRecruiter("scala.xml", "!", postJobComment)
    staffing.addRecruiter("scala.xml", "?", postJobProcInstr)
    staffing.addRecruiter("scala.xml.element", null, postJobElement)

    (() => {
      val namespaceName = "scala.xml"
      val localName = "xml"
      val workerDefn = postJobXml
      val args = scala.collection.immutable.Seq()
      (staffing.getRecruiter("scala.xml", "xml") match {
        case Some(f) => f(args)
        case None => q"nope"
      }).structure shouldBe workerDefn("scala.xml", "xml")(args).structure
    })()

    // TODO:  don't repeat yourself
    (() => {
      val namespaceName = "scala.xml"
      val localName = "dtd"
      val workerDefn: (String, String) => (scala.collection.immutable.Seq[Term.Arg]) => Tree = postJobDtd
      val args = scala.collection.immutable.Seq(Lit.String(""))
      staffing.getRecruiter(namespaceName, localName) match {
        case Some(f) => f(args).structure shouldBe workerDefn(namespaceName, localName)(args).structure
        case None => fail(s"Did not find a recruiter for $localName in namespace $namespaceName")
      }
    })()

    staffing.getRecruiter("scala.xml", "cdata") match {
      case Some(f) => f(scala.collection.immutable.Seq(Lit.String(""))).structure shouldBe postJobCdata("scala.xml", "cdata")(scala.collection.immutable.Seq(Lit.String(""))).structure
      case None => 3 shouldBe 5
    }

    staffing.getRecruiter("scala.xml", "!") match {
      case Some(f) => f(scala.collection.immutable.Seq(Lit.String(""))).structure shouldBe postJobComment("scala.xml", "!")(scala.collection.immutable.Seq(Lit.String(""))).structure
      case None => 3 shouldBe 5
    }

    staffing.getRecruiter("scala.xml", "?") match {
      case Some(f) => f(scala.collection.immutable.Seq(Lit.String(""))).structure shouldBe postJobProcInstr("scala.xml", "?")(scala.collection.immutable.Seq(Lit.String(""))).structure
      case None => 3 shouldBe 5
    }

    staffing.getRecruiter("scala.xml.element", "html") match {
      case Some(f) => f(scala.collection.immutable.Seq(Lit.String(""))).structure shouldBe postJobElement("scala.xml.element", "html")(scala.collection.immutable.Seq(Lit.String(""))).structure
      case None => 3 shouldBe 5
    }
  }
}