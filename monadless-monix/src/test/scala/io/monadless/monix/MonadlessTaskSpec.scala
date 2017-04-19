package io.monadless.monix

import java.util.concurrent.TimeUnit

import org.scalatest.MustMatchers

import io.monadless.impl.TestSupport
import monix.eval.Task
import monix.execution.Cancelable
import monix.execution.schedulers.ReferenceScheduler

class MonadlessTaskSpec
  extends org.scalatest.FreeSpec
  with MustMatchers
  with MonadlessTask
  with TestSupport[Task] {

  implicit val s = new ReferenceScheduler {
    def scheduleOnce(initialDelay: Long, unit: TimeUnit, r: Runnable) = {
      r.run()
      Cancelable.empty
    }
    def execute(command: Runnable) = command.run()
    def executionModel = monix.execution.ExecutionModel.SynchronousExecution
    def reportFailure(t: Throwable): Unit = {}
  }

  def get[T](f: Task[T]) =
    f.runSyncMaybe.right.get

  def fail[T]: T = throw new Exception

  val one = Task(1)
  val two = Task(2)

  "apply" in
    runLiftTest(1) {
      1
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
      runLiftTest(1) {
        try fail[Int]
        catch {
          case e: Exception => unlift(one)
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
