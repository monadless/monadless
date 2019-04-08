package io.monadless.lst

import scala.util.Failure
import scala.util.Try
import scala.util.Success
import scala.concurrent.Future
import scala.concurrent.Promise

object Effects {

  val optionEffect = new SyncEffect[Option] {
    def point[T](v: T) = Some(v)
    def lift[T](v: => T) = Option(v)
    def apply[T](o: Option[T]) =
      o match {
        case Some(v) => Sync(Left(v))
        case None    => Sync(Right(None))
      }
  }

  val tryEffect = new SyncEffect[Try] {
    def point[T](v: T) = Success(v)
    def lift[T](v: => T) = Try(v)
    def apply[T](o: Try[T]) =
      o match {
        case Success(v)  => Sync(Left(v))
        case Failure(ex) => Sync(Right(Failure(ex)))
      }
  }

  val futureEffect = new AsyncEffect[Future] {
    import scala.concurrent.ExecutionContext.Implicits.global
    def point[T](v: T) = Future.successful(v)
    def lift[T](v: => T) = Future(v)
    def async[T](r: Async[Future[T]]): Future[T] = {
      val p = Promise[T]()
      r.cb(p.completeWith(_))
      p.future
    }
    def apply[T](o: Future[T]) =
      Async { f =>
        o.onComplete {
          case Success(v)  => f(Left(v))
          case Failure(ex) => f(Right(Future.failed(ex)))
        }
      }
  }

}