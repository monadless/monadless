//package io.monadless
//
//class DefSpec extends Spec {
//
//  lift {
//        def a(i: Int) = unlift(lift(i + 1))
//        a(unlift(lift(1))) + 1
//      }
//  
////  "pure" - {
////    "no params" in
////      runLiftTest(3) {
////        def a = 2
////        unlift(lift(1)) + a
////      }
////    "one param" in
////      runLiftTest(3) {
////        def a(i: Int) = i + 1
////        unlift(lift(a(1))) + 1
////      }
////    "multiple params" in
////      runLiftTest(4) {
////        def a(i: Int, s: String) = i + s.toInt
////        unlift(lift(a(1, "2"))) + a(0, "1")
////      }
////    "multiple param groups" in
////      runLiftTest(4) {
////        def a(i: Int)(s: String) = i + s.toInt
////        unlift(lift(a(1)("2"))) + a(0)("1")
////      }
////    "nested" in
////      runLiftTest(5) {
////        def a(i: Int) = i + 1
////        def b(s: String) = s.toInt + a(2)
////        b("1") + 1
////      }
////  }
////
////  "unlifted" - {
////    "no params" in
////      runLiftTest(3) {
////        def a = unlift(lift(2))
////        unlift(lift(1)) + a
////      }
////    "one param" in
////      runLiftTest(3) {
////        def a(i: Int) = unlift(lift(i + 1))
////        a(1) + 1
////      }
////    "multiple params" in
////      runLiftTest(4) {
////        def a(i: Int, s: String) = unlift(lift(i)) + s.toInt
////        a(1, "2") + unlift(lift(1))
////      }
////    "multiple param groups" in
////      runLiftTest(4) {
////        def a(i: Int)(s: String) = unlift(lift(i)) + unlift(lift(s.toInt))
////        a(1)("2") + a(0)("1")
////      }
////    "unlifited param" in
////      lift {
////        def a(i: Int) = unlift(lift(i + 1))
////        a(unlift(lift(1))) + 1
////      }
////    "multiple methods" in
////      runLiftTest(5) {
////        def a(i: Int) = unlift(lift(i + 1))
////        def b(s: String) = unlift(lift(s.toInt)) + a(2)
////        b("1") + unlift(lift(1))
////      }
////    "nested" in
////      runLiftTest(2) {
////        def a1(s: String) = {
////          def a2(i: Int) = unlift(lift(i)) + 1
////          a2(unlift(lift(s.toInt)))
////        }
////        a1("1")
////      }
////    "nested object" in
////      runLiftTest(3) {
////        object A {
////          def a(i: Int) = unlift(lift(i + 1))
////        }
////        A.a(1) + 1
////      }
////    "nested class" in
////      runLiftTest(3) {
////        class A {
////          def a(i: Int) = unlift(lift(i + 1))
////        }
////        (new A).a(1) + 1
////      }
////    "nested trait" in
////      runLiftTest(3) {
////        trait A {
////          def a(i: Int) = unlift(lift(i + 1))
////        }
////        (new A {}).a(1) + 1
////      }
////    "recursive" in {
////      runLiftTest(0) {
////        def a(i: Int): Int = if (unlift(lift(i)) == 0) 0 else a(i - 1)
////        a(10)
////      }
////    }
////  }
//}