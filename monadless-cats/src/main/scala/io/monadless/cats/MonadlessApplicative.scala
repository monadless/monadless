package io.monadless.cats

import language.higherKinds
import io.monadless.Monadless
import cats.Applicative
import cats.Traverse

trait MonadlessApplicative[M[_]] extends Monadless[M] {

  protected val tc: Applicative[M]

  def apply[T](v: T): M[T] =
    tc.pure(v)

  def collect[T](list: List[M[T]])(implicit t: Traverse[List]): M[List[T]] =
    tc.sequence(list)

  def map[T, U](m: M[T])(f: T => U): M[U] =
    tc.map(m)(f)
}

object MonadlessApplicative {
  def apply[M[_]]()(implicit a: Applicative[M]) =
    new MonadlessApplicative[M] {
      override val tc = a
    }
}
