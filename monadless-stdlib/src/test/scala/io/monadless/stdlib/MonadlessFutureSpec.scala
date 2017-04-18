package io.monadless.stdlib

import org.scalatest.MustMatchers
import scala.concurrent.Future
import io.monadless.impl.TestSupport
import java.util.concurrent.atomic.AtomicReference
import scala.util.Try
import scala.concurrent.ExecutionContext

class MonadlessFutureSpec
  extends org.scalatest.FreeSpec
  with MustMatchers
  with MonadlessFuture
  with TestSupport[Future] {

  implicit val ec = new ExecutionContext {
    def execute(runnable: Runnable): Unit = runnable.run()
    def reportFailure(cause: Throwable): Unit = {}
  }

  def get[T](f: Future[T]) = {
    // can't use Await because of scala.js
    val r = new AtomicReference[Try[T]]
    f.onComplete(r.set)
    r.get.get
  }

  def fail[T]: T = throw new Exception

  val one = Future.successful(1)
  val two = Future.successful(2)

  "apply" in
    runLiftTest(1) {
      1
    }

  "collect" in
    runLiftTest(3) {
      unlift(one) + unlift(two)
    }

  "map" in
    runLiftTest(2) {
      unlift(one) + 1
    }

  "flatMap" in
    runLiftTest(3) {
      val a = unlift(one)
      a + unlift(two)
    }

  "rescue" - {
    "success" in
      runLiftTest(1) {
        try unlift(one)
        catch {
          case e: Throwable => unlift(two)
        }
      }
    "failure" in
      runLiftTest(1) {
        try fail[Int]
        catch {
          case e: Exception => unlift(one)
        }
      }
  }

  "ensure" - {
    "success" in
      runLiftTest(1) {
        var i = 0
        try unlift(one)
        finally i += 1
        i
      }
    "failure" in
      runLiftTest(1) {
        var i = 0
        try {
          try unlift(one) / fail[Int]
          finally i += 1
        } catch {
          case e: Exception => 1
        }
        i
      }
  }
}
