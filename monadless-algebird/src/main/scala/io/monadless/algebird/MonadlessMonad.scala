package io.monadless.algebird

import com.twitter.algebird.Monad

trait MonadlessMonad[M[_]] extends MonadlessApplicative[M] {

  override protected val tc: Monad[M]

  def flatMap[T, U](m: M[T])(f: T => M[U]): M[U] =
    tc.flatMap(m)(f)
}

object MonadlessMonad {
  def apply[M[_]]()(implicit a: Monad[M]) =
    new MonadlessMonad[M] {
      override protected val tc = a
    }
}
