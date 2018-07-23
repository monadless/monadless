package io.monadless.doobie

import cats.Applicative
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import io.monadless.Monadless
import doobie.syntax.monaderror._

trait MonadlessConnectionIO extends Monadless[ConnectionIO] {
  implicit val applicative = new ConnectionIOApplicative

  def apply[A](f: => A): ConnectionIO[A] = connection.pure(f)

  import cats.implicits._

  def collect[A](l: List[ConnectionIO[A]]): ConnectionIO[List[A]] = l.sequence

  def map[A, B](m: ConnectionIO[A])(f: A => B): ConnectionIO[B] = m.map(f)

  def flatMap[A, B](m: ConnectionIO[A])(f: A => ConnectionIO[B]): ConnectionIO[B] = m.flatMap(f)

  def rescue[A](m: ConnectionIO[A])(pf: PartialFunction[Throwable, ConnectionIO[A]]): ConnectionIO[A] = m.recoverWith(pf)

  def ensure[A](m: ConnectionIO[A])(f: => Unit): ConnectionIO[A] = m.guarantee(connection.pure(f))
}

object MonadlessConnectionIO$ extends MonadlessConnectionIO

class ConnectionIOApplicative extends Applicative[ConnectionIO] {
  override def pure[A](x: A): ConnectionIO[A] = connection.pure(x)

  override def ap[A, B](ff: ConnectionIO[A => B])(fa: ConnectionIO[A]): ConnectionIO[B] =
    for {
      a <- fa
      fatob <- ff
    } yield fatob.apply(a)
}
