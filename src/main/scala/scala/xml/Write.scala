package scala.xml

trait Write[T] {
  def writeAttribute(write: T)(localName: String, value: String): Unit
  def writeAttributeNs(write: T)(namespaceURI: String, localName: String, value: String): Unit
  def writeAttributePrefix(write: T)(prefix: String, namespaceURI: String, localName: String, value: String): Unit
  def writeCdata(write: T)(data: String): Unit
  def writeCharArray(write: T)(text: Array[Char], start: Int, len: Int): Unit
  def writeCharacters(write: T)(text: String): Unit
  def writeComment(write: T)(data: String): Unit
  def writeDefaultNamespace(write: T)(namespaceURI: String): Unit
  def writeDtd(write: T)(dtd: String): Unit
  def writeEmptyElement(write: T)(localName: String): Unit
  def writeEmptyElementNs(write: T)(namespaceURI: String, localName: String): Unit
  def writeEmptyElementPrefix(write: T)(prefix: String, localName: String, namespaceURI: String): Unit
  def writeEndDocument(write: T): Unit
  def writeEndElement(write: T): Unit
  def writeEntityRef(write: T)(name: String): Unit
  def writeNamespace(write: T)(prefix: String, namespaceURI: String): Unit
  def writeProcessingInstruction(write: T)(target: String): Unit
  def writeProcessingInstructionData(write: T)(target: String, data: String): Unit
  def writeStartDocument(write: T): Unit
  def writeStartDocumentVer(write: T)(version: String): Unit
  def writeStartDocumentVerEnc(write: T)(encoding: String, version: String): Unit
  def writeStartElement(write: T)(localName: String): Unit
  def writeStartElementNs(write: T)(namespaceURI: String, localName: String): Unit
  def writeStartElementPrefix(write: T)(prefix: String, localName: String, namespaceURI: String): Unit
  def cast(write: Any): T = write.asInstanceOf[T]
}

object Write {

  def apply[T](implicit w: Write[T]): Write[T] = w

  implicit class Ops[T: Write](t: T) {
    def writeAttribute: (String, String) => Unit = Write[T].writeAttribute(t)
    def writeAttributeNs: (String, String, String) => Unit = Write[T].writeAttributeNs(t)
    def writeAttributePrefix: (String, String, String, String) => Unit = Write[T].writeAttributePrefix(t)
    def writeCdata: String => Unit = Write[T].writeCdata(t)
    def writeCharArray: (Array[Char], Int, Int) => Unit = Write[T].writeCharArray(t)
    def writeCharacters: String => Unit = Write[T].writeCharacters(t)
    def writeComment: String => Unit = Write[T].writeComment(t)
    def writeDefaultNamespace: String => Unit = Write[T].writeDefaultNamespace(t)
    def writeDtd: String => Unit = Write[T].writeDtd(t)
    def writeEmptyElement: String => Unit = Write[T].writeEmptyElement(t)
    def writeEmptyElementNs: (String, String) => Unit = Write[T].writeEmptyElementNs(t)
    def writeEmptyElementPrefix: (String, String, String) => Unit = Write[T].writeEmptyElementPrefix(t)
    def writeEndDocument = Write[T].writeEndDocument(t)
    def writeEndElement = Write[T].writeEndElement(t)
    def writeEntityRef: String => Unit = Write[T].writeEntityRef(t)
    def writeNamespace: (String, String) => Unit = Write[T].writeNamespace(t)
    def writeProcessingInstruction: String => Unit = Write[T].writeProcessingInstruction(t)
    def writeProcessingInstructionData: (String, String) => Unit = Write[T].writeProcessingInstructionData(t)
    def writeStartDocument = Write[T].writeStartDocument(t)
    def writeStartDocumentVer: String => Unit = Write[T].writeStartDocumentVer(t)
    def writeStartDocumentVerEnc: (String, String) => Unit = Write[T].writeStartDocumentVerEnc(t)
    def writeStartElement: String => Unit = Write[T].writeStartElement(t)
    def writeStartElementNs: (String, String) => Unit = Write[T].writeStartElementNs(t)
    def writeStartElementPrefix: (String, String, String) => Unit = Write[T].writeStartElementPrefix(t)
  }
}
