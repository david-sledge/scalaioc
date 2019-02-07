package scala.xml

import javax.xml.stream.XMLStreamWriter

package object stream {

  implicit val writer: Writer[XMLStreamWriter] = new Writer[XMLStreamWriter] {

    override def writeAttribute(writer: XMLStreamWriter)(localName: String
        , value: String): Unit = writer.writeAttribute(localName, value)
    override def writeAttributeNs(writer: XMLStreamWriter)(namespaceURI: String
        , localName: String, value: String): Unit =
      writer.writeAttribute(namespaceURI, localName, value)
    override def writeAttributePrefix(writer: XMLStreamWriter)(prefix: String
        , namespaceURI: String, localName: String, value: String): Unit =
      writer.writeAttribute(prefix, namespaceURI, localName, value)
    override def writeCdata(writer: XMLStreamWriter)(data: String): Unit =
      writer.writeCData(data)
    override def writeCharArray(writer: XMLStreamWriter)(text: Array[Char]
        , start: Int, len: Int): Unit = writer.writeCharacters(text, start, len)
    override def writeCharacters(writer: XMLStreamWriter)(text: String): Unit =
      writer.writeCharacters(text)
    override def writeComment(writer: XMLStreamWriter)(data: String): Unit =
      writer.writeComment(data)
    override def writeDefaultNamespace(writer: XMLStreamWriter)
        (namespaceURI: String): Unit =
      writer.writeDefaultNamespace(namespaceURI)
    override def writeDtd(writer: XMLStreamWriter)(dtd: String): Unit =
      writer.writeDTD(dtd)
    override def writeEmptyElement(writer: XMLStreamWriter)
        (localName: String): Unit = writer.writeEmptyElement(localName)
    override def writeEmptyElementNs(writer: XMLStreamWriter)(namespaceURI: String
        , localName: String): Unit =
      writer.writeEmptyElement(namespaceURI, localName)
    override def writeEmptyElementPrefix(writer: XMLStreamWriter)(prefix: String
        , localName: String, namespaceURI: String): Unit =
      writer.writeEmptyElement(prefix, localName, namespaceURI)
    override def writeEndDocument(writer: XMLStreamWriter): Unit =
      writer.writeEndDocument
    override def writeEndElement(writer: XMLStreamWriter): Unit =
      writer.writeEndElement
    override def writeEntityRef(writer: XMLStreamWriter)(name: String): Unit =
      writer.writeEntityRef(name)
    override def writeNamespace(writer: XMLStreamWriter)(prefix: String
        , namespaceURI: String): Unit =
      writer.writeNamespace(prefix, namespaceURI)
    override def writeProcessingInstruction(writer: XMLStreamWriter)
        (target: String): Unit = writer.writeProcessingInstruction(target)
    override def writeProcessingInstructionData(writer: XMLStreamWriter)
        (target: String, data: String): Unit =
      writer.writeProcessingInstruction(target, data)
    override def writeStartDocument(writer: XMLStreamWriter): Unit =
      writer.writeStartDocument
    override def writeStartDocumentVer(writer: XMLStreamWriter)
        (version: String): Unit = writer.writeStartDocument(version)
    override def writeStartDocumentVerEnc(writer: XMLStreamWriter)(encoding: String
        , version: String): Unit = writer.writeStartDocument(encoding, version)
    override def writeStartElement(writer: XMLStreamWriter)
        (localName: String): Unit = writer.writeStartElement(localName)
    override def writeStartElementNs(writer: XMLStreamWriter)(namespaceURI: String
        , localName: String): Unit =
      writer.writeStartElement(namespaceURI, localName)
    override def writeStartElementPrefix(writer: XMLStreamWriter)(prefix: String
        , localName: String, namespaceURI: String): Unit =
      writer.writeStartElement(prefix, localName, namespaceURI)
  }
}
