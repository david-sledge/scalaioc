package scala.xml

  import stream.{writer => xmlStreamWriter}
  import Writer.Ops

import org.scalatest._
import javax.xml.stream.XMLOutputFactory

class WriterSpec extends FlatSpec with Matchers {
  "A Writer implementation" should "spit out XML" in {
    val strWriter = new java.io.StringWriter
    val w = XMLOutputFactory.newInstance.createXMLStreamWriter(strWriter)
    val obj: Any = (w, xmlStreamWriter)
    def f[T] = obj.asInstanceOf[(Any, Writer[T])]
    val (writer, tcImpl) = f
    import tcImpl._
    writeStartDocumentVer(cast(writer))("1.0")
    writeStartElement(cast(writer))("This")
    writeEndElement(cast(writer))
    writeEndDocument(cast(writer))
    strWriter.toString shouldBe "<?xml version=\"1.0\"?><This></This>"

    w.writeStartDocumentVer("1.1")
    w.writeStartElement("This")
    w.writeEndElement
    w.writeEndDocument
    strWriter.toString shouldBe "<?xml version=\"1.0\"?><This></This><?xml version=\"1.1\"?><This></This>"
  }
}
