package io.monadless.stdlib

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import io.monadless.Monadless

trait MonadlessFuture extends Monadless[Future] {

  def collect[T](list: List[Future[T]])(implicit ec: ExecutionContext): Future[List[T]] =
    Future.sequence(list)

  def rescue[T](m: Future[T])(pf: PartialFunction[Throwable, Future[T]])(implicit ec: ExecutionContext): Future[T] =
    m.recoverWith(pf)

  def ensure[T](m: Future[T])(f: => Unit)(implicit ec: ExecutionContext): Future[T] = {
    m.onComplete(_ => f)
    m
  }
}

object MonadlessFuture extends MonadlessFuture