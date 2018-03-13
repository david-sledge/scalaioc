package scala.xml.writer

import org.scalatest._
import javax.xml.stream.XMLOutputFactory

class XmlWriterTypeclassSpec extends FlatSpec with Matchers {
  "An XmlWriter implementation" should "spit out XML" in {
    //val wrtr = XMLOutputFactory.newInstance.createXMLStreamWriter(System.out)
    val obj: Any = (javax.xml.stream.XMLOutputFactory.newInstance.createXMLStreamWriter(System.out), XmlStreamWriter)
    //val (writer, tcImpl) = obj.asInstanceOf[(Any, XmlWriterTypeclass[T])]
    def f[T] = obj.asInstanceOf[(Any, XmlWriterTypeclass[T])]
    val (writer, tcImpl) = f
    import tcImpl._
    writeStartDocument(cast(writer), "1.0")
    writeStartElement(cast(writer), "This")
    writeEndElement(cast(writer))
    writeEndDocument(cast(writer))
  }
}
