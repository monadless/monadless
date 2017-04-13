package io.monadless

class WhileSpec extends Spec {

  "while" - {
    "unlifted condition" - {
      "pure body" in {
        runLiftTest(3) {
          var i = 0
          while (unlift(lift(i)) < 3)
            i += 1
          i
        }
      }
      "impure body" in {
        runLiftTest(3) {
          var i = 0
          while (unlift(lift(i)) < 3)
            i += unlift(lift(1))
          i
        }
      }
    }
    "pure condition" - {
      "pure body" in {
        runLiftTest(3) {
          var i = 0
          while (i < 3)
            i += 1
          i
        }
      }
      "impure body" in {
        runLiftTest(3) {
          var i = 0
          while (i < 3)
            i += unlift(lift(1))
          i
        }
      }
    }
  }

  "do while" - {
    "unlifted condition" - {
      "pure body" in {
        runLiftTest(1) {
          var i = 0
          do i += 1
          while (unlift(lift(i)) < 1)
          i
        }
      }
      "impure body" in {
        runLiftTest(3) {
          var i = 0
          do i += unlift(lift(1))
          while (unlift(lift(i)) < 3)
          i
        }
      }
    }
    "pure condition" - {
      "pure body" in {
        runLiftTest(3) {
          var i = 0
          do i += 1
          while (i < 3)
          i
        }
      }
      "impure body" in {
        runLiftTest(1) {
          var i = 0
          do i += 1
          while (i < 1)
          i
        }
      }
    }
  }
}