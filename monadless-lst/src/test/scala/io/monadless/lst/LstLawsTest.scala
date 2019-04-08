package io.monadless.lst

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Try
import scala.util.Success
import scala.concurrent.Future
import scala.concurrent.Promise

object LstLawsTest extends App {
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

  val v1 = Option(1)
  val v2 = Try(2)
  val v3 = Option(3)
  val v4 = Try(3)
  val v5 = Future.successful(5)

  {
    val eff =
      Lst[Stack.Two[Try, Option]] { eff =>
        for {
          v1 <- eff(v1)
          x = v1 + 1
          v2 <- eff(v2)
          y = v2 + 1
          v3 <- eff(v3)
          z = v3 + 3
          v4 <- eff(v4)
          h = v4 + 3
        } yield v1 + y + z + h
      }

    val r = eff.run(Stack.Two(tryEffect, optionEffect))
    println(r)
  }

  {
    val eff =
      Lst[Stack.Three[Future, Try, Option]] { eff =>
        for {
          v1 <- eff(v1)
          x = v1 + 1
          v2 <- eff(v2)
          y = v2 + 1
          v3 <- eff(v3)
          z = v3 + 3
          v4 <- eff(v4)
          h = v4 + 3
          v5 <- eff(v5)
          k = v5 + 1
        } yield v1 + y + z + h + k
      }

    val r = eff.run(Stack.Three(futureEffect, tryEffect, optionEffect))
    val a = Await.result(r, Duration.Inf)
    println(a)
  }
}