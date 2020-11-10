package io.monadless.lst

import scala.util.Try
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import Effects.optionEffect
import Effects.tryEffect
import cats.Monad
import cats.kernel.Eq
import cats.laws.discipline.MonadTests
import cats.laws.discipline.SemigroupalTests.Isomorphisms
import org.scalatest.funsuite.AnyFunSuiteLike
import org.typelevel.discipline.scalatest.Discipline
//import cats.tests.CatsSuite
//import org.scalatestplus.scalacheck.Checkers
import cats.implicits._

class LstLawsTest extends AnyFunSuiteLike with Discipline {

  abstract class Test[ST <: Stack[Any]](stack: ST) {
    type S = ST
    type F[T] = Lst[S, T]

    implicit val monad = new Monad[F] {
      def pure[A](x: A) = Lst.point(x)
      def flatMap[A, B](fa: F[A])(f: A => F[B]) = fa.flatMap(f)
      def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]) =
        f(a).flatMap {
          case Left(v)  => tailRecM(v)(f)
          case Right(v) => Lst.point(v)
        }
    }

    val eff = Lst[S]

    def eq[T](a: S#F[T], b: S#F[T]): Boolean

    implicit def feq[T]: Eq[F[T]] = Eq.instance {
      (a, b) =>
        val x = a.run(stack)
        val y = b.run(stack)
        eq(x, y)
    }

    implicit val iso = Isomorphisms.invariant[F]
  }

  new Test(Stack.Two(tryEffect, optionEffect)) {
    def eq[T](a: S#F[T], b: S#F[T]) = a == b
    implicit def fArbitrary[T](implicit o: Arbitrary[Option[T]], t: Arbitrary[Try[T]]): Arbitrary[F[T]] =
      Arbitrary {
        implicitly[Arbitrary[Int]].arbitrary.flatMap(i => Gen.oneOf[F[T]](o.arbitrary.map(eff(_)), t.arbitrary.map(eff(_))))
      }
    checkAll("Try[Option[T]]", MonadTests[F].monad[Int, Int, Int])
  }

  new Test(Stack.Two(optionEffect, tryEffect)) {
    def eq[T](a: S#F[T], b: S#F[T]) = a == b
    implicit def fArbitrary[T](implicit o: Arbitrary[Option[T]], t: Arbitrary[Try[T]]): Arbitrary[F[T]] =
      Arbitrary {
        implicitly[Arbitrary[Int]].arbitrary.flatMap(i => Gen.oneOf[F[T]](o.arbitrary.map(eff(_)), t.arbitrary.map(eff(_))))
      }
    checkAll("Option[Try[T]]", MonadTests[F].monad[Int, Int, Int])
  }

  //  new Test(Stack.Two(futureEffect, optionEffect)) {
  //    def eq[T](a: S#F[T], b: S#F[T]) = {
  //      val ax = Try(Await.result(a, Duration.Inf))
  //      val bx = Try(Await.result(b, Duration.Inf))
  //      if(ax != bx)
  //        println(1)
  //      ax == bx
  //    }
  //    implicit def fArbitrary[T](implicit o: Arbitrary[Option[T]], t: Arbitrary[Future[T]]): Arbitrary[F[T]] =
  //      Arbitrary {
  //        implicitly[Arbitrary[Int]].arbitrary.flatMap(i => Gen.oneOf[F[T]](o.arbitrary.map(eff(_)), t.arbitrary.map(eff(_))))
  //      }
  //    checkAll("Future[Option[T]]", MonadTests[F].monad[Int, Int, Int])
  //  }

  //  def tryOption() = {
  //
  //    type F[T] = Lst[Stack.Two[Try, Option], T]
  //
  //    implicit val tryOptionLstMonad = new Monad[F] {
  //      def pure[A](x: A) = Lst.point(x)
  //      def flatMap[A, B](fa: F[A])(f: A => F[B]) = fa.flatMap(f)
  //      def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]) =
  //        f(a).flatMap {
  //          case Left(v)  => tailRecM(v)(f)
  //          case Right(v) => Lst.point(v)
  //        }
  //    }
  //
  //    val stack = Stack.Two(tryEffect, optionEffect)
  //    val eff = Lst[Stack.Two[Try, Option]]
  //
  //    implicit def tryOptionLstArbitrary[T](implicit o: Arbitrary[Option[T]], t: Arbitrary[Try[T]]): Arbitrary[F[T]] =
  //      Arbitrary {
  //        implicitly[Arbitrary[Int]].arbitrary.flatMap(i => Gen.oneOf[F[T]](o.arbitrary.map(eff(_)), t.arbitrary.map(eff(_))))
  //      }
  //
  //    implicit def tryOptionLstEq[T]: Eq[F[T]] = Eq.instance {
  //      (a, b) =>
  //        val x = a.run(stack)
  //        val y = b.run(stack)
  //        if (x != y)
  //          println(1)
  //        x == y
  //    }
  //
  //    implicit val iso = Isomorphisms.invariant[F]
  //
  //    checkAll("Try >> Option", MonadTests[F].monad[Int, Int, Int])
  //  }

}