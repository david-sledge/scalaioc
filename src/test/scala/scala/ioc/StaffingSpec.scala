package scala.ioc

import scala.ioc.xml._
import org.scalatest._
import scala.meta._
import scala.collection.immutable.Seq

class StaffingSpec extends FlatSpec with Matchers {
  "The IoC transformer" should "manipulate the scala AST" in {
    val staffing = Staffing()
    staffing.transformIoC(q""""id" `#scala.ioc#=` "value"""").structure should be
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

    def testWorkerDef(namespaceName: String, localName: String,
        workerDefn: (String, String) => (Term, Seq[Term.Arg]) => Tree,
        args: Seq[Term.Arg]) = {
      staffing.getRecruiter(namespaceName, localName) match {
        case Some(f) => f(null, args).structure shouldBe workerDefn(namespaceName, localName)(null, args).structure
        case None => fail(s"Did not find a recruiter for $localName in namespace $namespaceName")
      }
    }

    testWorkerDef("scala.xml", "xml", postJobXml, Seq())
    testWorkerDef("scala.xml", "dtd", postJobDtd, Seq(Lit.String("")))
    testWorkerDef("scala.xml", "cdata", postJobCdata, Seq(Lit.String("")))
    testWorkerDef("scala.xml", "!", postJobComment, Seq(Lit.String("")))
    testWorkerDef("scala.xml", "?", postJobProcInstr, Seq(Lit.String("")))
    testWorkerDef("scala.xml.element", "html", postJobElement, Seq(Lit.String("")))

    staffing.hasRecruiter("scala.xml", "dtd") shouldBe true
    staffing.hasOwnRecruiter("scala.xml", "dtd") shouldBe true
    staffing.hasRecruiter("scala.xml.element", "dtd") shouldBe true
    staffing.hasOwnRecruiter("scala.xml.element", "dtd") shouldBe false
    staffing.hasRecruiter("scala.xml", null) shouldBe false
    staffing.hasOwnRecruiter("scala.xml", null) shouldBe false

    staffing.hasRecruiter("scala.xml.element", "dtd") shouldBe true
    staffing.hasOwnRecruiter("scala.xml.element", "dtd") shouldBe false
    staffing.hasRecruiter("scala.xml.element", null) shouldBe true
  }
}