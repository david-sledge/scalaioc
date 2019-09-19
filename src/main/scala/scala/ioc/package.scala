package scala

package object ioc {

  def cast[T](x: Any): T = x.asInstanceOf[T]

}
