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

  def collect[T](list: List[Try[T]]): Try[List[T]] =
    list.foldLeft(Try(List.empty[T])) {
      (acc, item) =>
        for {
          l <- acc
          i <- item
        } yield l :+ i
    }

  def get[T](m: Try[T]): T = m.get
  
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