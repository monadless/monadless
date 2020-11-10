package io.monadless

import language.experimental.macros

trait Monadless[Monad[_]] {

  type M[T] = Monad[T]

  /* "ghost" methods

    def apply[T](v: => T): M[T]
    def collect[T](list: List[M[T]]): M[List[T]]
    def rescue[T](m: M[T])(pf: PartialFunction[Throwable, M[T]]): M[T]
    def ensure[T](m: M[T])(f: => Unit): M[T]

  */

  def lift[T](body: T): Monad[T] = macro impl.Macro.lift[Monad, T]

  def unlift[T](m: M[T]): T = throw new Exception("Unlift must be used within a `lift` body.")
}

object Monadless {
  def apply[M[_]](): Monadless[M] = new Monadless[M] {}
}
