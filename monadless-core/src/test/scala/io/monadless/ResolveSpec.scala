package io.monadless

import org.scalatest.MustMatchers

class ResolveSpec
  extends org.scalatest.FreeSpec
  with MustMatchers {

  class WithMonadMethods[T](val calls: List[String]) {
    def map[U](f: T => U) =
      new WithMonadMethods[U](calls :+ "WithMonadMethods.map")
    def flatMap[U](f: T => WithMonadMethods[U]) =
      new WithMonadMethods[U](calls :+ "WithMonadMethods.flatMap")
    def rescue(pf: PartialFunction[Throwable, WithMonadMethods[T]]) =
      new WithMonadMethods[T](calls :+ "WithMonadMethods.rescue")
    def ensure(f: => Unit) =
      new WithMonadMethods[T](calls :+ "WithMonadMethods.ensure")
  }

  object WithMonadMethods {
    def apply[T](v: => T) = new WithMonadMethods[T]("WithMonadMethods.apply" :: Nil)
    def collect[T](list: List[WithMonadMethods[T]]) = new WithMonadMethods[List[T]]("WithMonadMethods.collect" :: Nil)
  }

  class WithoutMonadMethods[T](val calls: List[String])

  "instance method" - {
    val monadless = Monadless[WithMonadMethods]
    import monadless._

    "apply" in {
      val m = lift(1)
      m.calls mustEqual List("WithMonadMethods.apply")
    }

    "collect" in {
      val m = lift {
        (unlift(lift(1)), unlift(lift(2)))
      }
      m.calls mustEqual List("WithMonadMethods.collect", "WithMonadMethods.map")
    }

    "map" in {
      val m = lift {
        val a = unlift(lift(1))
        a + 1
      }
      m.calls mustEqual List("WithMonadMethods.apply", "WithMonadMethods.map")
    }

    "flatMap" in {
      val m = lift {
        val a = unlift(lift(1))
        unlift(lift(a + 1))
      }
      m.calls mustEqual List("WithMonadMethods.apply", "WithMonadMethods.flatMap")
    }

    "rescue" in {
      val m = lift {
        try unlift(lift(1))
        catch {
          case e: Throwable => 2
        }
      }
      m.calls mustEqual List("WithMonadMethods.apply", "WithMonadMethods.rescue")
    }

    "ensure" in {
      val m = lift {
        try unlift(lift(1))
        finally {
          println(1)
        }
      }
      m.calls mustEqual List("WithMonadMethods.apply", "WithMonadMethods.ensure")
    }
  }

  "monadless method" - {
    object monadless extends Monadless[WithoutMonadMethods] {
      def map[T, U](m: WithoutMonadMethods[T])(f: T => U) =
        new WithoutMonadMethods[U](m.calls :+ "monadless.map")
      def flatMap[T, U](m: WithoutMonadMethods[T])(f: T => WithoutMonadMethods[U]) =
        new WithoutMonadMethods[U](m.calls :+ "monadless.flatMap")
      def rescue[T](m: WithoutMonadMethods[T])(pf: PartialFunction[Throwable, WithoutMonadMethods[T]]) =
        new WithoutMonadMethods[T](m.calls :+ "monadless.rescue")
      def ensure[T](m: WithoutMonadMethods[T])(f: => Unit) =
        new WithoutMonadMethods[T](m.calls :+ "monadless.ensure")
      def apply[T](v: => T) = new WithoutMonadMethods[T]("monadless.apply" :: Nil)
      def collect[T](list: List[WithoutMonadMethods[T]]) = new WithoutMonadMethods[List[T]]("monadless.collect" :: Nil)
    }
    import monadless._

    "apply" in {
      val m = lift(1)
      m.calls mustEqual List("monadless.apply")
    }

    "collect" in {
      val m = lift {
        (unlift(lift(1)), unlift(lift(2)))
      }
      m.calls mustEqual List("monadless.collect", "monadless.map")
    }

    "map" in {
      val m = lift {
        val a = unlift(lift(1))
        a + 1
      }
      m.calls mustEqual List("monadless.apply", "monadless.map")
    }

    "flatMap" in {
      val m = lift {
        val a = unlift(lift(1))
        unlift(lift(a + 1))
      }
      m.calls mustEqual List("monadless.apply", "monadless.flatMap")
    }

    "rescue" in {
      val m = lift {
        try unlift(lift(1))
        catch {
          case e: Throwable => 2
        }
      }
      m.calls mustEqual List("monadless.apply", "monadless.rescue")
    }

    "ensure" in {
      val m = lift {
        try unlift(lift(1))
        finally {
          println(1)
        }
      }
      m.calls mustEqual List("monadless.apply", "monadless.ensure")
    }
  }

  "monadless method has precedence over instance method" - {
    object monadless extends Monadless[WithMonadMethods] {
      def map[T, U](m: WithMonadMethods[T])(f: T => U) =
        new WithMonadMethods[U](m.calls :+ "monadless.map")
      def flatMap[T, U](m: WithMonadMethods[T])(f: T => WithMonadMethods[U]) =
        new WithMonadMethods[U](m.calls :+ "monadless.flatMap")
      def rescue[T](m: WithMonadMethods[T])(pf: PartialFunction[Throwable, WithMonadMethods[T]]) =
        new WithMonadMethods[T](m.calls :+ "monadless.rescue")
      def ensure[T](m: WithMonadMethods[T])(f: => Unit) =
        new WithMonadMethods[T](m.calls :+ "monadless.ensure")
      def apply[T](v: => T) = new WithMonadMethods[T]("monadless.apply" :: Nil)
      def collect[T](list: List[WithMonadMethods[T]]) = new WithMonadMethods[List[T]]("monadless.collect" :: Nil)
    }
    import monadless._

    "apply" in {
      val m = lift(1)
      m.calls mustEqual List("monadless.apply")
    }

    "collect" in {
      val m = lift {
        (unlift(lift(1)), unlift(lift(2)))
      }
      m.calls mustEqual List("monadless.collect", "monadless.map")
    }

    "map" in {
      val m = lift {
        val a = unlift(lift(1))
        a + 1
      }
      m.calls mustEqual List("monadless.apply", "monadless.map")
    }

    "flatMap" in {
      val m = lift {
        val a = unlift(lift(1))
        unlift(lift(a + 1))
      }
      m.calls mustEqual List("monadless.apply", "monadless.flatMap")
    }

    "rescue" in {
      val m = lift {
        try unlift(lift(1))
        catch {
          case e: Throwable => 2
        }
      }
      m.calls mustEqual List("monadless.apply", "monadless.rescue")
    }

    "ensure" in {
      val m = lift {
        try unlift(lift(1))
        finally {
          println(1)
        }
      }
      m.calls mustEqual List("monadless.apply", "monadless.ensure")
    }
  }

  "fails if method is not present" - {
    val monadless = new Monadless[WithoutMonadMethods] {}
    import monadless._

    "apply" in {
      "lift(1)" mustNot compile
    }

    "collect" in {
      """
    	val a = new WithoutMonadMethods[Int](Nil)
    	val b = new WithoutMonadMethods[Int](Nil)
      lift {
        (unlift(a), unlift(b))
      }
      """ mustNot compile
    }

    "map" in {
      """
    	val a = new WithoutMonadMethods[Int](Nil)
      lift {
        val b = unlift(a)
        b + 1
      }
      """ mustNot compile
    }

    "flatMap" in {
      """
    	val a = new WithoutMonadMethods[Int](Nil)
    	val b = new WithoutMonadMethods[Int](Nil)
      lift {
        unlift(a)
        unlift(b)
      }
      """ mustNot compile
    }

    "rescue" in {
      """
    	val a = new WithoutMonadMethods[Int](Nil)
      lift {
        try unlift(a)
        catch {
          case e: Throwable => 2
        }
      }
      """ mustNot compile
    }

    "ensure" in {
      """
    	val a = new WithoutMonadMethods[Int](Nil)
      lift {
        try unlift(a)
        finally {
          println(1)
        }
      }
      """ mustNot compile
    }
  }

  "default monadless" - {
    "no parenthesis" in {
      val m = Monadless[Option]
      import m._

      val option =
        lift {
          val a = unlift(Option(1))
          val b = unlift(Option(2))
          a + b
        }

      option mustEqual Some(3)
    }
    "with parenthesis" in {
      val m = Monadless[Option]()
      import m._

      val option =
        lift {
          val a = unlift(Option(1))
          val b = unlift(Option(2))
          a + b
        }

      option mustEqual Some(3)
    }
  }
}