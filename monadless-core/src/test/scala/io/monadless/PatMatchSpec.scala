package io.monadless

class PatMatchSpec extends Spec {

  "unlifted scrutinee" - {
    "without guards" - {
      "pure cases" in
        runLiftTest(3) {
          unlift(lift("b")) match {
            case "a" => 2
            case "b" => 3
          }
        }
      "pure/impure cases" in
        runLiftTest(2) {
          unlift(lift("a")) match {
            case "a" => unlift(lift(2))
            case "b" => 3
          }
        }
      "impure cases" in
        runLiftTest(3) {
          unlift(lift("b")) match {
            case "a" => unlift(lift(2))
            case "b" => unlift(lift(3))
          }
        }
    }
    "with guards" - {
      "pure cases" in
        runLiftTest(3) {
          unlift(lift("b")) match {
            case s if s == "a" => 2
            case "b"           => 3
          }
        }
      "pure/impure cases" in
        runLiftTest(2) {
          unlift(lift("a")) match {
            case "a"           => unlift(lift(2))
            case s if s == "b" => 3
          }
        }
      "impure cases" in
        runLiftTest(2) {
          unlift(lift("b")) match {
            case s if "1".toInt == 1 => unlift(lift(2))
            case "b"                 => unlift(lift(3))
          }
        }
    }
  }

  "pure scrutinee" - {
    "without guards" - {
      "pure cases" in
        runLiftTest(3) {
          "b" match {
            case "a" => 2
            case "b" => 3
          }
        }
      "pure/impure cases" in
        runLiftTest(2) {
          unlift(lift("a")) match {
            case "a" => unlift(lift(2))
            case "b" => 3
          }
        }
      "impure cases" in
        runLiftTest(3) {
          "b" match {
            case "a" => unlift(lift(2))
            case "b" => unlift(lift(3))
          }
        }
    }
    "with guards" - {
      "pure cases" in
        runLiftTest(3) {
          "b" match {
            case s if s == "a" => 2
            case "b"           => 3
          }
        }
      "pure/impure cases" in
        runLiftTest(2) {
          unlift(lift("a")) match {
            case "a"           => unlift(lift(2))
            case s if s == "b" => 3
          }
        }
      "impure cases" in
        runLiftTest(2) {
          "b" match {
            case s if "1".toInt == 1 => unlift(lift(2))
            case "b"                 => unlift(lift(3))
          }
        }
    }
  }

  "val patmatch" in
    runLiftTest(1) {
      val Some(a) = unlift(lift(Some(1)))
      a
    }

  "invalid unlifted guard" in pendingUntilFixed {
    runLiftTest(2) {
      "b" match {
        case s if unlift(lift(true)) => 2
        case "b"                     => unlift(lift(3))
      }
    }
  }
}