package io.monadless

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

class MonadlessFuture extends Monadless[Future] {
  def apply[T](v: => T)(implicit ec: ExecutionContext): Future[T] = Future(v)

  def join[T1, T2](m1: Future[T1], m2: Future[T2]): Future[(T1, T2)] = m1.zip(m2)

  def get[T](m: Future[T]): T = Await.result(m, 1.seconds)

  def handle[T](m: Future[T])(pf: PartialFunction[Throwable, T])(implicit ec: ExecutionContext): Future[T] = m.recover(pf)

  def rescue[T](m: Future[T])(pf: PartialFunction[Throwable, Future[T]])(implicit ec: ExecutionContext): Future[T] = m.recoverWith(pf)

  def ensure[T](m: Future[T])(f: => Unit)(implicit ec: ExecutionContext): Unit = m.onComplete { _ => f }
}
