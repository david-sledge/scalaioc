package scala.xml.writer

trait XmlWriter[T] {
  def writeAttribute(writer: T)(localName: String, value: String): Unit
  def writeAttributeNs(writer: T)(namespaceURI: String, localName: String, value: String): Unit
  def writeAttributePrefix(writer: T)(prefix: String, namespaceURI: String, localName: String, value: String): Unit
  def writeCdata(writer: T)(data: String): Unit
  def writeCharArray(writer: T)(text: Array[Char], start: Int, len: Int): Unit
  def writeCharacters(writer: T)(text: String): Unit
  def writeComment(writer: T)(data: String): Unit
  def writeDefaultNamespace(writer: T)(namespaceURI: String): Unit
  def writeDtd(writer: T)(dtd: String): Unit
  def writeEmptyElement(writer: T)(localName: String): Unit
  def writeEmptyElementNs(writer: T)(namespaceURI: String, localName: String): Unit
  def writeEmptyElementPrefix(writer: T)(prefix: String, localName: String, namespaceURI: String): Unit
  def writeEndDocument(writer: T): Unit
  def writeEndElement(writer: T): Unit
  def writeEntityRef(writer: T)(name: String): Unit
  def writeNamespace(writer: T)(prefix: String, namespaceURI: String): Unit
  def writeProcessingInstruction(writer: T)(target: String): Unit
  def writeProcessingInstructionData(writer: T)(target: String, data: String): Unit
  def writeStartDocument(writer: T): Unit
  def writeStartDocumentVer(writer: T)(version: String): Unit
  def writeStartDocumentVerEnc(writer: T)(encoding: String, version: String): Unit
  def writeStartElement(writer: T)(localName: String): Unit
  def writeStartElementNs(writer: T)(namespaceURI: String, localName: String): Unit
  def writeStartElementPrefix(writer: T)(prefix: String, localName: String, namespaceURI: String): Unit
  def cast(writer: Any): T = writer.asInstanceOf[T]
}

object XmlWriter {

  def apply[T](implicit w: XmlWriter[T]): XmlWriter[T] = w

  object ops {

    def writeComment[T: XmlWriter](t: T): String => Unit
      = XmlWriter[T].writeComment(t)

    implicit class XmlWriterOps[T: XmlWriter](t: T) {
      def writeAttribute: (String, String) => Unit = XmlWriter[T].writeAttribute(t)
      def writeAttributeNs: (String, String, String) => Unit = XmlWriter[T].writeAttributeNs(t)
      def writeAttributePrefix: (String, String, String, String) => Unit = XmlWriter[T].writeAttributePrefix(t)
      def writeCdata: String => Unit = XmlWriter[T].writeCdata(t)
      def writeCharArray: (Array[Char], Int, Int) => Unit = XmlWriter[T].writeCharArray(t)
      def writeCharacters: String => Unit = XmlWriter[T].writeCharacters(t)
      def writeComment: String => Unit = XmlWriter[T].writeComment(t)
      def writeDefaultNamespace: String => Unit = XmlWriter[T].writeDefaultNamespace(t)
      def writeDtd: String => Unit = XmlWriter[T].writeDtd(t)
      def writeEmptyElement: String => Unit = XmlWriter[T].writeEmptyElement(t)
      def writeEmptyElementNs: (String, String) => Unit = XmlWriter[T].writeEmptyElementNs(t)
      def writeEmptyElementPrefix: (String, String, String) => Unit = XmlWriter[T].writeEmptyElementPrefix(t)
      def writeEndDocument = XmlWriter[T].writeEndDocument(t)
      def writeEndElement = XmlWriter[T].writeEndElement(t)
      def writeEntityRef: String => Unit = XmlWriter[T].writeEntityRef(t)
      def writeNamespace: (String, String) => Unit = XmlWriter[T].writeNamespace(t)
      def writeProcessingInstruction: String => Unit = XmlWriter[T].writeProcessingInstruction(t)
      def writeProcessingInstructionData: (String, String) => Unit = XmlWriter[T].writeProcessingInstructionData(t)
      def writeStartDocument = XmlWriter[T].writeStartDocument(t)
      def writeStartDocumentVer: String => Unit = XmlWriter[T].writeStartDocumentVer(t)
      def writeStartDocumentVerEnc: (String, String) => Unit = XmlWriter[T].writeStartDocumentVerEnc(t)
      def writeStartElement: String => Unit = XmlWriter[T].writeStartElement(t)
      def writeStartElementNs: (String, String) => Unit = XmlWriter[T].writeStartElementNs(t)
      def writeStartElementPrefix: (String, String, String) => Unit = XmlWriter[T].writeStartElementPrefix(t)
    }
  }
}

import javax.xml.stream.XMLStreamWriter

object JavaXmlStreamWriter {

  implicit val javaXmlStreamWriter: XmlWriter[XMLStreamWriter] = new XmlWriter[XMLStreamWriter]{

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
