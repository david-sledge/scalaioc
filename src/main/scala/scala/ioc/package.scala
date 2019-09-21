package scala

package object ioc {

  // workaround for https://github.com/scala/bug/issues/11700
  def cast[T](x: Any): T = x.asInstanceOf[T]

}
