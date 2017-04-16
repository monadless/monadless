package io.monadless.stdlib

import io.monadless.Monadless
import scala.util.control.NonFatal

object MonadlessOption extends Monadless {

  type M[T] = Option[T]

  def apply[T](v: => T) = Option(v)

  def collect[T](list: List[Option[T]]): Option[List[T]] =
    list.foldLeft(Option(List.empty[T])) {
      (acc, item) =>
        for {
          l <- acc
          i <- item
        } yield l :+ i
    }

  //  def rescue[T](m: Option[T])(pf: PartialFunction[Throwable, Option[T]]) = m.recoverWith(pf)

  def ensure[T](m: Option[T])(f: => Unit) =
    m.map { r =>
      try f
      catch {
        case NonFatal(e) => ()
      }
      r
    }
}