package scala.xml

  import stream.{write => streamWriteXml}
  import Write.Ops

import org.scalatest._
import javax.xml.stream.XMLOutputFactory

class WriteSpec extends FlatSpec with Matchers {
  "A Write implementation" should "spit out XML" in {
    val strWriter = new java.io.StringWriter
    val w = XMLOutputFactory.newInstance.createXMLStreamWriter(strWriter)
    val obj: Any = (w, streamWriteXml)
    def f[T] = obj.asInstanceOf[(Any, Write[T])]
    val (writer, write) = f
    import write._
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
