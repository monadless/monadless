package io.monadless.stdlib

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import io.monadless.Monadless

object MonadlessFuture extends Monadless {

  type M[T] = Future[T]

  def apply[T](v: => T)(implicit ec: ExecutionContext): Future[T] =
    Future.apply(v)

  def collect[T](list: List[Future[T]])(implicit ec: ExecutionContext): Future[List[T]] =
    Future.sequence(list)

  def rescue[T](m: Future[T])(pf: PartialFunction[Throwable, Future[T]])(implicit ec: ExecutionContext): Future[T] =
    m.recoverWith(pf)

  def ensure[T](m: Future[T])(f: => Unit)(implicit ec: ExecutionContext): Future[T] = {
    m.onComplete(_ => f)
    m
  }
}