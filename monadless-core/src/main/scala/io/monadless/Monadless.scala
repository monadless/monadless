package io.monadless

import language.experimental.macros
import language.higherKinds

trait Monadless {

  type M[T]

  /* "ghost" methods 

    def apply[T](v: => T): M[T]
    def collect[T](list: M[T]): M[List[T]]
    def get[T](m: M[T]): T
    def handle[T](m: M[T])(pf: PartialFunction[Throwable, T]): M[T]
    def rescue[T](m: M[T])(pf: PartialFunction[Throwable, M[T]]): M[T]
    def ensure[T](m: M[T])(f: => Unit): M[T]
  
  */

  def lift[T](body: T): M[T] = macro impl.Macro.lift[T]

  def unlift[T](m: M[T]): T = ???
}
