package io.monadless

class TrySpec extends Spec {

  val e = new Exception

  "failure" - {
    "try pure" - {
      "catch pure" in
        runLiftTest(2) {
          try throw e
          catch {
            case `e` => 2
          }
        }
      "catch impure" in
        runLiftTest(2) {
          try throw e
          catch {
            case `e` => unlift(lift(2))
          }
        }
      "catch pure/impure" in
        runLiftTest(1) {
          try throw e
          catch {
            case `e`          => 1
            case _: Throwable => unlift(lift(2))
          }
        }
    }
  }

  "success" - {
    "try pure" - {
      "catch pure" in
        runLiftTest(1) {
          try 1
          catch {
            case `e` => 2
          }
        }
      "catch impure" in
        runLiftTest(1) {
          try 1
          catch {
            case `e` => unlift(lift(2))
          }
        }
      "catch pure/impure" in
        runLiftTest(1) {
          try 1
          catch {
            case `e`          => 2
            case _: Throwable => unlift(lift(3))
          }
        }
    }
  }

  "finally" - {
    "pure" in
      runLiftTest(true) {
        var called = false
        val _ =
          try unlift(lift(1))
          finally {
            called = true
          }
        called
      }
    "without catch" in
      runLiftTest(true) {
        var called = false
        try
          unlift(lift(1))
        finally
          called = unlift(lift(true))
        called
      }
    "as the only impure" in
      runLiftTest(true) {
        var called = false
        val _ =
          try 1
          catch {
            case `e`          => 2
            case _: Throwable => 3
          } finally {
            called = unlift(lift(true))
          }
        called
      }
  }
}