package scala.util

final class IteratorEnumeration[T](
  iterator: scala.collection.Iterator[T]
) extends java.util.Enumeration[T] {
  def hasMoreElements() = iterator.hasNext
  def nextElement() = iterator.next
}
