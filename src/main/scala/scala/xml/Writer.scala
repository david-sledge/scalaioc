package scala.xml

trait Writer[T] {
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

object Writer {

  def apply[T](implicit w: Writer[T]): Writer[T] = w

  implicit class Ops[T: Writer](t: T) {
    def writeAttribute: (String, String) => Unit = Writer[T].writeAttribute(t)
    def writeAttributeNs: (String, String, String) => Unit = Writer[T].writeAttributeNs(t)
    def writeAttributePrefix: (String, String, String, String) => Unit = Writer[T].writeAttributePrefix(t)
    def writeCdata: String => Unit = Writer[T].writeCdata(t)
    def writeCharArray: (Array[Char], Int, Int) => Unit = Writer[T].writeCharArray(t)
    def writeCharacters: String => Unit = Writer[T].writeCharacters(t)
    def writeComment: String => Unit = Writer[T].writeComment(t)
    def writeDefaultNamespace: String => Unit = Writer[T].writeDefaultNamespace(t)
    def writeDtd: String => Unit = Writer[T].writeDtd(t)
    def writeEmptyElement: String => Unit = Writer[T].writeEmptyElement(t)
    def writeEmptyElementNs: (String, String) => Unit = Writer[T].writeEmptyElementNs(t)
    def writeEmptyElementPrefix: (String, String, String) => Unit = Writer[T].writeEmptyElementPrefix(t)
    def writeEndDocument = Writer[T].writeEndDocument(t)
    def writeEndElement = Writer[T].writeEndElement(t)
    def writeEntityRef: String => Unit = Writer[T].writeEntityRef(t)
    def writeNamespace: (String, String) => Unit = Writer[T].writeNamespace(t)
    def writeProcessingInstruction: String => Unit = Writer[T].writeProcessingInstruction(t)
    def writeProcessingInstructionData: (String, String) => Unit = Writer[T].writeProcessingInstructionData(t)
    def writeStartDocument = Writer[T].writeStartDocument(t)
    def writeStartDocumentVer: String => Unit = Writer[T].writeStartDocumentVer(t)
    def writeStartDocumentVerEnc: (String, String) => Unit = Writer[T].writeStartDocumentVerEnc(t)
    def writeStartElement: String => Unit = Writer[T].writeStartElement(t)
    def writeStartElementNs: (String, String) => Unit = Writer[T].writeStartElementNs(t)
    def writeStartElementPrefix: (String, String, String) => Unit = Writer[T].writeStartElementPrefix(t)
  }
}
