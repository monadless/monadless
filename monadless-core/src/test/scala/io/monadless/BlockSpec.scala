package io.monadless

class BlockSpec extends Spec {

  "assigned unlift" - {
    "only" in {
      val i = lift(1)
      runLiftTest(()) {
        val v = unlift(i)
      }
    }
    "followed by pure expression" in {
      val i = lift(1)
      runLiftTest(2) {
        val v = unlift(i)
        v + 1
      }
    }
    "followed by impure expression" in {
      val i = lift(1)
      val j = lift(2)
      runLiftTest(3) {
        val v = unlift(i)
        v + unlift(j)
      }
    }
    "nested" in {
      val i = lift(1)
      runLiftTest(3) {
        val v = {
          val r = unlift(i)
          r + 1
        }
        v + 1
      }
    }
  }

  "unassigned unlift" - {
    "only" in {
      val i = lift(1)
      runLiftTest(1) {
        unlift(i)
      }
    }
    "followed by pure expression" in {
      val i = lift(1)
      runLiftTest(2) {
        unlift(i)
        2
      }
    }
    "followed by impure expression" in {
      val i = lift(1)
      val j = lift(2)
      runLiftTest(2) {
        unlift(i)
        unlift(j)
      }
    }
  }

  "pure expression" - {
    "only" in {
      runLiftTest(1) {
        1
      }
    }
    "followed by pure expression" in {
      val i = lift(1)
      def a = 1
      runLiftTest(2) {
        a
        2
      }
    }
    "followed by impure expression" in {
      val i = lift(1)
      def a = 2
      runLiftTest(1) {
        a
        unlift(i)
      }
    }
  }

  "nested" in {
    val i = lift(1)
    runLiftTest(4) {
      var x = 0
      x += {
        val v = unlift(i) + 2
        v + 1
      }
      val r =
        lift {
          unlift(i)
          x - 1
        }
      unlift(r) + 1
    }
  }
}