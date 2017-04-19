package io.monadless.monix

import io.monadless.Monadless
import monix.eval.Task

trait MonadlessTask extends Monadless[Task] {

  def collect[T](list: List[Task[T]]): Task[List[T]] =
    Task.gather(list)

  def rescue[T](m: Task[T])(pf: PartialFunction[Throwable, Task[T]]): Task[T] =
    m.onErrorRecoverWith(pf)

  def ensure[T](m: Task[T])(f: => Unit): Task[T] =
    m.doOnFinish(_ => Task(f))
}

object MonadlessTask extends MonadlessTask