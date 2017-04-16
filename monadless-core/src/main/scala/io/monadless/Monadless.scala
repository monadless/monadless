package io.monadless

import language.experimental.macros
import language.higherKinds

trait Monadless[M[_]] {

  /* "ghost" methods 

    def apply[T](v: => T): M[T]
    def join[T1, T2](m1: M[T1], m2: M[T2]): M[(T1, T2)]
    def get[T](m: M[T]): T
    def handle[T](m: M[T])(pf: PartialFunction[Throwable, T]): M[T]
    def rescue[T](m: M[T])(pf: PartialFunction[Throwable, M[T]]): M[T]
    def ensure[T](m: M[T])(f: => Unit): M[T]
  
  */

  def lift[T](body: T): M[T] = macro impl.Macro.lift[M, T]

  def unlift[T](m: M[T]): T = ???
}
