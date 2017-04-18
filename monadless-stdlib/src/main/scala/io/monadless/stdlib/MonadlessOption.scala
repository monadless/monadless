package io.monadless.stdlib

import io.monadless.Monadless

trait MonadlessOption extends Monadless[Option] {

  def collect[T](list: List[Option[T]]): Option[List[T]] =
    list.foldLeft(Option(List.empty[T])) {
      (acc, item) =>
        for {
          l <- acc
          i <- item
        } yield l :+ i
    }
}

object MonadlessOption extends MonadlessOption