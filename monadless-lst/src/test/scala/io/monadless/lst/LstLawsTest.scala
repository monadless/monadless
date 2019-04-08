package io.monadless.lst

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Try
import scala.util.Success
import scala.concurrent.Future
import scala.concurrent.Promise
import Effects._
import cats.tests.CatsSuite
import cats.laws.discipline.MonadTests
import cats.Monad
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import cats.kernel.Eq
import org.scalacheck.Cogen
import cats.laws.discipline.SemigroupalTests.Isomorphisms

class LstLawsTest extends CatsSuite {

  type TryOptionLst[T] = Lst[Stack.Two[Try, Option], T]

  implicit val tryOptionLstMonad = new Monad[TryOptionLst] {
    def pure[A](x: A) = Lst.point(x)
    def flatMap[A, B](fa: TryOptionLst[A])(f: A => TryOptionLst[B]) = fa.flatMap(f)
    def tailRecM[A, B](a: A)(f: A => TryOptionLst[Either[A, B]]) =
      f(a).flatMap {
        case Left(v)  => tailRecM(v)(f)
        case Right(v) => Lst.point(v)
      }
  }

  val stack = Stack.Two(tryEffect, optionEffect)
  val eff = Lst[Stack.Two[Try, Option]]

  implicit def tryOptionLstArbitrary[T](implicit o: Arbitrary[Option[T]], t: Arbitrary[Try[T]]): Arbitrary[TryOptionLst[T]] =
    Arbitrary {
      implicitly[Arbitrary[Int]].arbitrary.flatMap(i => Gen.oneOf[TryOptionLst[T]](o.arbitrary.map(eff(_)), t.arbitrary.map(eff(_))))
    }

  implicit def tryOptionLstEq[T]: Eq[TryOptionLst[T]] = Eq.instance {
    (a, b) =>
      val x = a.run(stack)
      val y = b.run(stack)
      if(x != y)
        println(1)
      x == y
  }

  implicit val iso = Isomorphisms.invariant[TryOptionLst]

  checkAll("TryOptionLst[Int]", MonadTests[TryOptionLst].monad[Int, Int, Int])

  val v1 = Option(1)
  val v2 = Try(2)
  val v3 = Option(3)
  val v4 = Try(3)
  val v5 = Future.successful(5)

  {
    val eff =
      Lst[Stack.Two[Try, Option]] { eff =>
        for {
          v1 <- eff(v1)
          x = v1 + 1
          v2 <- eff(v2)
          y = v2 + 1
          v3 <- eff(v3)
          z = v3 + 3
          v4 <- eff(v4)
          h = v4 + 3
        } yield v1 + y + z + h
      }

    val r = eff.run(Stack.Two(tryEffect, optionEffect))
    println(r)
  }

  {
    val eff =
      Lst[Stack.Three[Future, Try, Option]] { eff =>
        for {
          v1 <- eff(v1)
          x = v1 + 1
          v2 <- eff(v2)
          y = v2 + 1
          v3 <- eff(v3)
          z = v3 + 3
          v4 <- eff(v4)
          h = v4 + 3
          v5 <- eff(v5)
          k = v5 + 1
        } yield v1 + y + z + h + k
      }

    val r = eff.run(Stack.Three(futureEffect, tryEffect, optionEffect))
    val a = Await.result(r, Duration.Inf)
    println(a)
  }
}