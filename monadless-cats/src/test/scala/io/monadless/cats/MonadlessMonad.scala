package io.monadless.cats

//import cats.instances.list._
import org.scalatest.MustMatchers

import io.monadless.impl.TestSupport

import cats.instances.list._
import cats.Id
import cats.Monad

class MonadlessMonadSpec
  extends org.scalatest.FreeSpec
  with MustMatchers
  with MonadlessMonad[Id]
  with TestSupport[Id] {

  override protected val tc = implicitly[Monad[Id]]

  def get[T](t: Id[T]): T = t

  def fail[T]: T = throw new Exception

  val one: Id[Int] = 1
  val two: Id[Int] = 2

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

  "flatMap" in {
    runLiftTest(3) {
      val a = unlift(one)
      a + unlift(two)
    }
  }

  "rescue" - {
    "success" in {
      """
      lift {
        try unlift(one)
        catch {
          case e: Exception => unlift(two)
        }
      }
      """ mustNot compile
    }
    "failure" in {
      """
      lift {
        try fail[Int]
        catch {
          case e: Exception => unlift(one)
        }
      }
      """ mustNot compile
    }
  }

  "ensure" - {
    "success" in {
      """
      lift {
        var i = 0
        try unlift(one)
        finally i += 1
        i
      }
      """ mustNot compile
    }
    "failure" in {
      """
      lift {
        var i = 0
        try {
          try unlift(one) / fail[Int]
          finally i += 1
        } catch {
          case e: Exception => 1
        }
        i
      }
      """ mustNot compile
    }
  }
}
