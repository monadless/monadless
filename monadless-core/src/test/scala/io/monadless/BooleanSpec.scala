package io.monadless

class BooleanSpec extends Spec {

  // avoids scalac optimizations
  def True = "1".toInt == 1
  def False = "1".toInt == 0
  def NotExpected: Boolean = ???

  "&&" - {
    "pure/pure" in
      runLiftTest(False) {
        1 == 1 && 2 == 3
      }
    "pure/impure" - {
      "True/True" in
        runLiftTest(True) {
          True && unlift(lift(True))
        }
      "True/False" in
        runLiftTest(False) {
          True && unlift(lift(False))
        }
      "False/NotExpected" in
        runLiftTest(False) {
          False && unlift(lift(NotExpected))
        }
    }
    "impure/pure" - {
      "True/True" in
        runLiftTest(True) {
          unlift(lift(True)) && True
        }
      "True/False" in
        runLiftTest(False) {
          unlift(lift(True)) && False
        }
      "False/NotExpected" in
        runLiftTest(False) {
          unlift(lift(False)) && NotExpected
        }
    }
    "impure/impure" - {
      "True/True" in
        runLiftTest(True) {
          unlift(lift(True)) && unlift(lift(True))
        }
      "True/False" in
        runLiftTest(False) {
          unlift(lift(True)) && unlift(lift(False))
        }
      "False/NotExpected" in
        runLiftTest(False) {
          unlift(lift(False)) && unlift(lift(NotExpected))
        }
    }
  }

  "||" - {
    "pure/pure" in
      runLiftTest(True) {
        1 == 1 || 2 == 3
      }
    "pure/impure" - {
      "False/False" in
        runLiftTest(False) {
          False || unlift(lift(False))
        }
      "False/True" in
        runLiftTest(True) {
          False || unlift(lift(True))
        }
      "True/NotExpected" in
        runLiftTest(True) {
          True || unlift(lift(NotExpected))
        }
    }
    "impure/pure" - {
      "False/False" in
        runLiftTest(False) {
          unlift(lift(False)) || False
        }
      "False/True" in
        runLiftTest(True) {
          unlift(lift(False)) || True
        }
      "True/NotExpected" in
        runLiftTest(True) {
          unlift(lift(True)) || NotExpected
        }
    }
    "impure/impure" - {
      "False/False" in
        runLiftTest(False) {
          unlift(lift(False)) || unlift(lift(False))
        }
      "False/True" in
        runLiftTest(True) {
          unlift(lift(False)) || unlift(lift(True))
        }
      "True/NotExpected" in
        runLiftTest(True) {
          unlift(lift(True)) || unlift(lift(NotExpected))
        }
    }
  }
}