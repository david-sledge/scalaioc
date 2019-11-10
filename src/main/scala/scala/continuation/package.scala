package scala

package object continuation {
  final case class OptionC[T, R](onNone: () => R, onSome: T => R)

  def contOption[T, R](value: T, onNone: => R, onSome: T => R) = {

    if (value == null)
      onNone
    else
      onSome(value)
  }

  final case class EitherC[L, R, A](onLeft: L => A, onRight: R => A)
}
