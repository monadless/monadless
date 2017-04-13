package io.monadless

class IfSpec extends Spec {

  "unlifted condition / ifelse" - {
    "pure / pure" in
      runLiftTest(2) {
        if (unlift(lift(1)) == 1) 2 else 3
      }
    "pure / impure" in
      runLiftTest(2) {
        if (unlift(lift(1)) == 1) unlift(lift(2)) else 3
      }
    "impure / pure" in
      runLiftTest(2) {
        if (unlift(lift(1)) == 1) 2 else unlift(lift(3))
      }
    "impure / impure" in
      runLiftTest(3) {
        if (unlift(lift(1)) == 2) unlift(lift(2)) else unlift(lift(3))
      }
  }

  "pure condition / ifelse" - {
    "pure / pure" in
      runLiftTest(2) {
        if (1 == 1) 2 else 3
      }
    "pure / impure" in
      runLiftTest(2) {
        if (1 == 1) unlift(lift(2)) else 3
      }
    "impure / pure" in
      runLiftTest(2) {
        if (1 == 1) 2 else unlift(lift(3))
      }
    "impure / impure" in
      runLiftTest(3) {
        if (1 == 2) unlift(lift(2)) else unlift(lift(3))
      }
  }
}
