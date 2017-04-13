package io.monadless

class UnsupportedSpec extends Spec {

  "unlifted guard" in pendingUntilFixed {
    runLiftTest(1) {
      1 match {
        case i if unlift(lift(1)) == i => 1
      }
    }
  }

  "return" - {
    "only" in pendingUntilFixed {
      def t: Any =
        runLiftTest(1) {
          return 1
        }
      t
    }
    "nested" in pendingUntilFixed {
      runLiftTest(1) {
        def t: Int = return 1
        t
      }
    }
  }

  "lazy val" in pendingUntilFixed {
    runLiftTest(2) {
      lazy val v = unlift(lift(1))
      v + 1
    }
  }

  "by-name param" - {
    "one param" in pendingUntilFixed {
      def m(a: => Int) = a
      runLiftTest(1) {
        m(unlift(lift(1)))
      }
    }
    "two params" in pendingUntilFixed {
      def m(a: Int, b: => Int) = a + b
      runLiftTest(2) {
        m(1, unlift(lift(2)))
      }
    }
    "param groups" in pendingUntilFixed {
      def m(a: Int)(b: => Int) = a + b
      runLiftTest(2) {
        m(1)(unlift(lift(2)))
      }
    }
  }

  "functions" in pendingUntilFixed {
    runLiftTest(1) {
      val f = (i: Int) => unlift(lift(i))
      f(1)
    }
  }

  "overriden method" - {
    "trait" in pendingUntilFixed {
      trait A[T] {
        def a: Int
      }

      runLiftTest(1) {
        val v = new A[Int] {
          def a = unlift(lift(1))
        }
        v.a
      }
    }

    "partial function" in pendingUntilFixed {
      runLiftTest(1) {
        val pf: PartialFunction[Int, Int] = {
          case 1 => unlift(lift(1))
          case 2 => 3
        }
        pf(1)
      }
    }
  }

  "def type param" in pendingUntilFixed {
    runLiftTest(1) {
      def a[T](v: T) = unlift[T](lift[T](v))
      a(1)
    }
  }

  //  "unlifted constructor initializer"
}