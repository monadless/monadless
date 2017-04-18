package io.monadless

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

class MonadlessFuture extends Monadless[Future] {
  def apply[T](v: => T)(implicit ec: ExecutionContext): Future[T] = Future(v)

  def collect[T](list: List[Future[T]])(implicit ec: ExecutionContext): Future[List[T]] = Future.sequence(list)

  def get[T](m: Future[T]): T = Await.result(m, 1.seconds)

  def handle[T](m: Future[T])(pf: PartialFunction[Throwable, T])(implicit ec: ExecutionContext): Future[T] = m.recover(pf)

  def rescue[T](m: Future[T])(pf: PartialFunction[Throwable, Future[T]])(implicit ec: ExecutionContext): Future[T] = m.recoverWith(pf)

  def ensure[T](m: Future[T])(f: => Unit)(implicit ec: ExecutionContext): Unit = m.onComplete { _ => f }
}
