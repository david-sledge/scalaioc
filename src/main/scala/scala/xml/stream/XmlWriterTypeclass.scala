package scala.xml.stream

trait XmlWriterTypeclass[T] {
  def writeAttribute(writer: T, localName: String, value: String): Unit
  def writeAttribute(writer: T, namespaceURI: String, localName: String, value: String): Unit
  def writeAttribute(writer: T, prefix: String, namespaceURI: String, localName: String, value: String): Unit
  def writeCData(writer: T, data: String): Unit
  def writeCharacters(writer: T, text: Array[Char], start: Int, len: Int): Unit
  def writeCharacters(writer: T, text: String): Unit
  def writeComment(writer: T, data: String): Unit
  def writeDefaultNamespace(writer: T, namespaceURI: String): Unit
  def writeDTD(writer: T, dtd: String): Unit
  def writeEmptyElement(writer: T, localName: String): Unit
  def writeEmptyElement(writer: T, namespaceURI: String, localName: String): Unit
  def writeEmptyElement(writer: T, prefix: String, localName: String, namespaceURI: String): Unit
  def writeEndDocument(writer: T): Unit
  def writeEndElement(writer: T): Unit
  def writeEntityRef(writer: T, name: String): Unit
  def writeNamespace(writer: T, prefix: String, namespaceURI: String): Unit
  def writeProcessingInstruction(writer: T, target: String): Unit
  def writeProcessingInstruction(writer: T, target: String, data: String): Unit
  def writeStartDocument(writer: T): Unit
  def writeStartDocument(writer: T, version: String): Unit
  def writeStartDocument(writer: T, encoding: String, version: String): Unit
  def writeStartElement(writer: T, localName: String): Unit
  def writeStartElement(writer: T, namespaceURI: String, localName: String): Unit
  def writeStartElement(writer: T, prefix: String, localName: String, namespaceURI: String): Unit
  def cast(writer: Any): T = writer.asInstanceOf[T]
}

import javax.xml.stream.XMLStreamWriter

object XmlStreamWriter extends XmlWriterTypeclass[XMLStreamWriter] {
  override def writeAttribute(writer: XMLStreamWriter, localName: String
      , value: String): Unit = writer.writeAttribute(localName, value)
  override def writeAttribute(writer: XMLStreamWriter, namespaceURI: String
      , localName: String, value: String): Unit =
    writer.writeAttribute(namespaceURI, localName, value)
  override def writeAttribute(writer: XMLStreamWriter, prefix: String
      , namespaceURI: String, localName: String, value: String): Unit =
    writer.writeAttribute(prefix, namespaceURI, localName, value)
  override def writeCData(writer: XMLStreamWriter, data: String): Unit =
    writer.writeCData(data)
  override def writeCharacters(writer: XMLStreamWriter, text: Array[Char]
      , start: Int, len: Int): Unit = writer.writeCharacters(text, start, len)
  override def writeCharacters(writer: XMLStreamWriter, text: String): Unit =
    writer.writeCharacters(text)
  override def writeComment(writer: XMLStreamWriter, data: String): Unit =
    writer.writeComment(data)
  override def writeDefaultNamespace(writer: XMLStreamWriter
      , namespaceURI: String): Unit =
    writer.writeDefaultNamespace(namespaceURI)
  override def writeDTD(writer: XMLStreamWriter, dtd: String): Unit =
    writer.writeDTD(dtd)
  override def writeEmptyElement(writer: XMLStreamWriter
      , localName: String): Unit = writer.writeEmptyElement(localName)
  override def writeEmptyElement(writer: XMLStreamWriter, namespaceURI: String
      , localName: String): Unit =
    writer.writeEmptyElement(namespaceURI, localName)
  override def writeEmptyElement(writer: XMLStreamWriter, prefix: String
      , localName: String, namespaceURI: String): Unit =
    writer.writeEmptyElement(prefix, localName, namespaceURI)
  override def writeEndDocument(writer: XMLStreamWriter): Unit = {
    writer.writeEndDocument
    writer.flush
  }
  override def writeEndElement(writer: XMLStreamWriter): Unit =
    writer.writeEndElement
  override def writeEntityRef(writer: XMLStreamWriter, name: String): Unit =
    writer.writeEntityRef(name)
  override def writeNamespace(writer: XMLStreamWriter, prefix: String
      , namespaceURI: String): Unit =
    writer.writeNamespace(prefix, namespaceURI)
  override def writeProcessingInstruction(writer: XMLStreamWriter
      , target: String): Unit = writer.writeProcessingInstruction(target)
  override def writeProcessingInstruction(writer: XMLStreamWriter
      , target: String, data: String): Unit =
    writer.writeProcessingInstruction(target, data)
  override def writeStartDocument(writer: XMLStreamWriter): Unit =
    writer.writeStartDocument
  override def writeStartDocument(writer: XMLStreamWriter
      , version: String): Unit = writer.writeStartDocument(version)
  override def writeStartDocument(writer: XMLStreamWriter, encoding: String
      , version: String): Unit = writer.writeStartDocument(encoding, version)
  override def writeStartElement(writer: XMLStreamWriter
      , localName: String): Unit = writer.writeStartElement(localName)
  override def writeStartElement(writer: XMLStreamWriter, namespaceURI: String
      , localName: String): Unit =
    writer.writeStartElement(namespaceURI, localName)
  override def writeStartElement(writer: XMLStreamWriter, prefix: String
      , localName: String, namespaceURI: String): Unit =
    writer.writeStartElement(prefix, localName, namespaceURI)
}
