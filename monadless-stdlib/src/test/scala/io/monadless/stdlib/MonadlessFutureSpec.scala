package io.monadless.stdlib

import org.scalatest.MustMatchers
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import io.monadless.impl.TestSupport
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MonadlessFutureSpec
  extends org.scalatest.FreeSpec
  with MustMatchers
  with MonadlessFuture
  with TestSupport[Future] {

  def get[T](f: Future[T]) = Await.result(f, Duration.Inf)

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
        try 1 / 0
        catch {
          case e: Throwable => unlift(one)
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
          try unlift(one) / 0
          finally i += 1
        } catch {
          case e: ArithmeticException => 1
        }
        i
      }
  }
}
