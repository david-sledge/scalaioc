package scala.xml.stream

import JavaXmlStreamWriter.javaXmlStreamWriter
import XmlWriter.ops.XmlWriterOps
import org.scalatest._
import javax.xml.stream.XMLOutputFactory

class XmlWriterSpec extends FlatSpec with Matchers {
  "An XmlWriter implementation" should "spit out XML" in {
    val w = XMLOutputFactory.newInstance.createXMLStreamWriter(System.out)
    val obj: Any = (w, javaXmlStreamWriter)
    //val (writer, tcImpl) = obj.asInstanceOf[(Any, XmlWriterTypeclass[T])]
    def f[T] = obj.asInstanceOf[(Any, XmlWriter[T])]
    val (writer, tcImpl) = f
    import tcImpl._
    writeStartDocumentVer(cast(writer))("1.0")
    writeStartElement(cast(writer))("This")
    writeEndElement(cast(writer))
    writeEndDocument(cast(writer))

    println()

    w.writeStartDocumentVer("1.1")
    w.writeStartElement("This")
    w.writeEndElement
    w.writeEndDocument
    w.flush

    println()
  }
}
