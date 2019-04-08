package io.monadless.algebird

import org.scalatest.MustMatchers

import io.monadless.impl.TestSupport
import com.twitter.algebird.Identity
import com.twitter.algebird.Applicative

class MonadlessApplicativeSpec
  extends org.scalatest.FreeSpec
  with MustMatchers
  with MonadlessApplicative[Identity]
  with TestSupport[Identity] {

  override protected val tc = implicitly[Applicative[Identity]]

  def get[T](t: Identity[T]): T = t.get

  def fail[T]: T = throw new Exception

  val one = Identity(1)
  val two = Identity(2)

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
    """
    lift {
      val a = unlift(one)
      a + unlift(two)
    }
    """ mustNot compile
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
        def c() = i += 1
        try unlift(one)
        finally {
          c()
        }
        i
      }
      """ mustNot compile
    }
    "failure" in {
      """
      lift {
        var i = 0
        def c() = i += 1
        try {
          try unlift(one) / fail[Int]
          finally {
            c()
          }
        } catch {
          case e: Exception => 1
        }
        i
      }
      """ mustNot compile
    }
  }
}
