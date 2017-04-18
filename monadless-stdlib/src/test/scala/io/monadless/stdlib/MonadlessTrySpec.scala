package io.monadless.stdlib

import scala.util.Try

import org.scalatest.MustMatchers

import io.monadless.impl.TestSupport

class MonadlessTrySpec
  extends org.scalatest.FreeSpec
  with MustMatchers
  with MonadlessTry
  with TestSupport[Try] {

  def get[T](t: Try[T]) = t.get

  def fail[T]: T = throw new Exception

  val one = Try(1)
  val two = Try(2)

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
          case e: Exception => unlift(two)
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
