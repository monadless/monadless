package io.monadless

import scala.util.Try
import scala.util.control.NonFatal

import org.scalatest.MustMatchers
import io.monadless.impl.TestSupport

trait Spec
  extends org.scalatest.FreeSpec
  with MustMatchers
  with Monadless
  with TestSupport[Try] {

  type M[T] = Try[T]
  
  def apply[T](v: => T) = Try(v)
  def join[T1, T2](m1: Try[T1], m2: Try[T2]) =
    for {
      a <- m1
      b <- m2
    } yield (a, b)
  def get[T](m: Try[T]): T = m.get
  def handle[T](m: Try[T])(pf: PartialFunction[Throwable, T]) = m.recover(pf)
  def rescue[T](m: Try[T])(pf: PartialFunction[Throwable, Try[T]]) = m.recoverWith(pf)
  def ensure[T](m: Try[T])(f: => Unit) =
    m.map { r =>
      try f
      catch {
        case NonFatal(e) => ()
      }
      r
    }
}