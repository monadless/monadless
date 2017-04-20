package io.monadless.algebird

import language.higherKinds
import io.monadless.Monadless
import com.twitter.algebird.Applicative

trait MonadlessApplicative[M[_]] extends Monadless[M] {

  protected val tc: Applicative[M]

  def apply[T](v: T): M[T] =
    tc.apply(v)

  def collect[T](list: List[M[T]]): M[List[T]] =
    tc.map(tc.sequence(list))(_.toList)

  def map[T, U](m: M[T])(f: T => U): M[U] =
    tc.map(m)(f)
}

object MonadlessApplicative {
  def apply[M[_]]()(implicit a: Applicative[M]) =
    new MonadlessApplicative[M] {
      override val tc = a
    }
}
