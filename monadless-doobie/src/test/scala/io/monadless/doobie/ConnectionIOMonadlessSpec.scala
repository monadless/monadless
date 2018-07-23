package io.monadless.doobie

import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import io.monadless.impl.TestSupport
import org.scalatest.MustMatchers

class ConnectionIOMonadlessSpec extends org.scalatest.FreeSpec
  with MustMatchers
  with MonadlessConnectionIO
  with TestSupport[ConnectionIO] {

  import doobie.implicits._

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  private def queryInt(i: Int): ConnectionIO[Int] =
    sql"""select ${i} as result""".query[Int].to[List].map(_.head)

  val one: ConnectionIO[Int] = queryInt(1)
  val two: ConnectionIO[Int] = queryInt(2)

  override def get[T](m: ConnectionIO[T]): T = m.transact(xa).unsafeRunSync()

  def fail[T]: T = throw new Exception

  "apply" in
    runLiftTest(1) {
      unlift(one)
    }

  "collect" in
    runLiftTest(3) {
      unlift(one) + unlift(two)
    }

  "map" in
    runLiftTest(2) {
      unlift(one) + 1
    }

  "flatMap" in
    runLiftTest(3) {
      val a = unlift(one)
      a + unlift(two)
    }

  "rescue" - {
    "success" in
      runLiftTest(1) {
        try unlift(one)
        catch {
          case e: Throwable => unlift(two)
        }
      }
    "failure" in
      runLiftTest(1337) {
        try unlift(two) / fail[Int]
        catch {
          case e: Exception => unlift(queryInt(1337))
        }
      }
  }

  "ensure" - {
    "success" in
      runLiftTest(1) {
        var i = 0
        try unlift(one)
        finally i += 1
        i
      }
    "failure" in
      runLiftTest(1) {
        var i = 0
        try {
          try unlift(one) / fail[Int]
          finally i += 1
        } catch {
          case e: Exception => 1
        }
        i
      }
  }
}
