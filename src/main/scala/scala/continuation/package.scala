package scala

package object continuation {
  final case class OptionC[T, R](onNone: () => R, onSome: T => R)

  def contOption[T, R](value: T, onNone: => R, onSome: T => R): R = {

    if (value == null)
      onNone
    else
      onSome(value)
  }

  final case class EitherC[L, R, A](onLeft: L => A, onRight: R => A)

  def fix[A](f: (=> A) => A): A = {
    lazy val a: A = f(a)
    a
  }
}
