package io.monadless.lst

import scala.Left
import scala.Right
import language.higherKinds
import language.implicitConversions

trait Effect[F[+_]] {
  def point[T](v: T): F[T]
  def lift[T](v: => T): F[T]
  def apply[T](o: F[T]): Result[Either[T, F[Nothing]]]
}

trait AsyncEffect[F[+_]] extends Effect[F] {
  def async[T](r: Async[F[T]]): F[T]
}

trait SyncEffect[F[+_]] extends Effect[F] {
}

sealed trait Result[+T] {
  def map[U](f: T => U): Result[U] = flatMap(v => Sync(f(v)))
  def flatMap[U](f: T => Result[U]): Result[U] =
    this match {
      case Sync(v) => f(v)
      case Async(cb) =>
        Async[U] { g =>
          cb { v =>
            f(v) match {
              case Sync(v)   => g(v)
              case Async(cb) => cb(g)
            }
          }
        }
    }
}
case class Sync[T](v: T) extends Result[T]
case class Async[T](cb: (T => Unit) => Unit) extends Result[T]

sealed trait Stack[+T[_]] {
  type F[U] <: T[U]
}
object Stack {
  case class One[E1[+_]](e1: Effect[E1]) extends Stack[E1]
  case class Two[E1[+_], E2[+_]](e1: Effect[E1], e2: Effect[E2])
    extends Stack[({ type T[A] = E1[E2[A]] })#T]
  case class Three[E1[+_], E2[+_], E3[+_]](e1: Effect[E1], e2: Effect[E2], e3: Effect[E3])
    extends Stack[({ type T[A] = E1[E2[E3[A]]] })#T]

  implicit def one[E1[+_]](s: One[E1]): Effect[E1] = s.e1
  implicit def twoFirst[E1[+_], E2[+_]](s: Two[E1, E2]): Effect[E1] = s.e1
  implicit def twoSecond[E1[+_], E2[+_]](s: Two[E1, E2]): Effect[E2] = s.e2
  implicit def threeFirst[E1[+_], E2[+_], E3[+_]](s: Three[E1, E2, E3]): Effect[E1] = s.e1
  implicit def threeSecond[E1[+_], E2[+_], E3[+_]](s: Three[E1, E2, E3]): Effect[E2] = s.e2
  implicit def threeThird[E1[+_], E2[+_], E3[+_]](s: Three[E1, E2, E3]): Effect[E3] = s.e3
}

sealed trait Lst[S <: Stack[Any], +T] {

  def map[U](f: T => U): Lst[S, U] = flatMap(v => Lst.point(f(v)))

  def flatMap[U](f: T => Lst[S, U]): Lst[S, U] =
    this match {
      case v: Lst.Value[S, T]   => Lst.FlatMap(v, f)
      case Lst.FlatMap(v, cont) => Lst.FlatMap[S, Any, U](v, (x: Any) => cont(x).flatMap(f))
    }

  def run[B >: T](stack: S): S#F[B] = {

    def runEffects(effects: List[Effect[Any]], eff: Lst[S, Any]): Result[Either[Any, Lst.EffValue[S, Any, Any]]] = {
      effects match {
        case Nil => stage(eff)
        case head :: tail =>
          runEffect[Any, Any](head, runEffects(tail, eff))
      }
    }

    def runEffect[F[+_], U](effect: Effect[F], cont: => Result[Either[U, Lst.EffValue[S, Any, U]]]): Result[Either[F[U], Lst.EffValue[S, Any, U]]] =
      effect(effect.lift(cont)).flatMap {
        case Right(f) => Sync(Left(f))
        case Left(r) =>
          r.map {
            case Left(v) => Left(effect.point(v))
            case Right(ev) =>
              ev.effect match {
                case `effect` => Left(ev.v.asInstanceOf[F[U]])
                case _        => Right(ev)
              }
          }
      }

    def stage(eff: Lst[S, Any]): Result[Either[Any, Lst.EffValue[S, Any, Any]]] = {
      eff match {
        case Lst.Point(v)                 => Sync(Left(v))
        case ev @ Lst.EffValue(_, _)      => Sync(Right(ev))
        case Lst.FlatMap(Lst.Point(v), f) => stage(f(v))
        case Lst.FlatMap(Lst.EffValue(effectF, v), f) =>
          effectF(stack)(v).flatMap {
            (_: Either[Any, Any]) match {
              case Left(v)  => stage(f(v))
              case Right(f) => Sync(Right(Lst.EffValue[S, Any, Any](effectF, f)))
            }
          }
      }
    }

    val effects =
      stack match {
        case Stack.One(e1)           => e1 :: Nil
        case Stack.Two(e1, e2)       => e1 :: e2 :: Nil
        case Stack.Three(e1, e2, e3) => e1 :: e2 :: e3 :: Nil
      }

    def fail[U](msg: String): U = throw new IllegalStateException(msg)

    val result = runEffects(effects, this).asInstanceOf[Result[Either[S#F[B], Lst.EffValue[S, Any, Any]]]]

    val extracted =
      result.map {
        case Left(f)   => f
        case Right(ev) => fail(s"Lst bug: effect not handled ${ev.v}")
      }

    extracted match {
      case Sync(f) => f
      case a: Async[_] =>
        effects.head match {
          case effect: AsyncEffect[_] =>
            effect.async(a.asInstanceOf[Async[Any]]).asInstanceOf[S#F[B]]
          case _ =>
            // TODO should be checked by the type system
            fail("first effect must be async if any of the effects is async")
        }
    }
  }
}

object Lst {
  class Unlift[S <: Stack[Any]] {
    def apply[F[+_], T](v: F[T])(implicit cont: S => Effect[F]) =
      EffValue(cont, v)
  }
  class StackBuilder[S <: Stack[Any]] {
    def apply[T](f: Unlift[S] => Lst[S, T]) = f(new Unlift[S])
  }
  def apply[S <: Stack[Any]] = new StackBuilder[S]
  def point[S <: Stack[Any], T](v: T): Lst[S, T] = Point[S, T](v)

  sealed trait Value[S <: Stack[Any], T] extends Lst[S, T]
  case class EffValue[S <: Stack[Any], F[+_], T](effect: S => Effect[F], v: F[T]) extends Value[S, T]
  case class Point[S <: Stack[Any], T](v: T) extends Value[S, T]

  case class FlatMap[S <: Stack[Any], T, U](v: Value[S, T], f: T => Lst[S, U]) extends Lst[S, U]
}
