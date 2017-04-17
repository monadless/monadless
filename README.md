# Monadless

Monadless is syntactic sugar for monad composition. Or, "async/await"
(see SIP-22) generalized. Or -- a way to write monad-heavy functional code
in a way that looks procedural.

[![Build Status](https://travis-ci.org/monadless/monadless.svg?branch=master)](https://travis-ci.org/monadless/monadless)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/ea4068928617433f8275534af3351152)](https://www.codacy.com/app/fwbrasil/monadless)
[![codecov.io](https://codecov.io/github/monadless/monadless/coverage.svg?branch=master)](https://codecov.io/github/monadless/monadless?branch=master)
[![Join the chat at https://gitter.im/monadless/monadless](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/monadless/onadless?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)-[![Dependency Status](https://www.versioneye.com/user/projects/58f1b1915c12c800161e64d1/badge.svg?style=flat)](https://www.versioneye.com/user/projects/58f1b1915c12c800161e64d1)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.monadless/monadless_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.monadless/monadless_2.11)
[![Javadocs](https://www.javadoc.io/badge/io.monadless/monadless_2.11.svg)](https://www.javadoc.io/doc/io.monadless/monadless-core_2.11)

Code of Conduct
---------------

Please note that this project is released with a Contributor Code of Conduct. By participating in this project you agree to abide by its terms. See [CODE_OF_CONDUCT.md](https://github.com/monadless/monadless/blob/master/CODE_OF_CONDUCT.md) for details.

License
-------

See the [LICENSE](https://github.com/monadless/monadless/blob/master/LICENSE.txt) file for details.

Maintainers
===========

- @fwbrasil
- @sameerparekh

## Problem

Dealing with monad compositions involves considerable syntax noise. For instance, this code using the Future monad:

```scala
callServiceA().flatMap { a =>
  callServiceB(a).flatMap { b =>
    callServiceC(b).map { c =>
      (a, c)
    }
  }
}
```


would be much easier to follow using synchronous operations, without a monad:

```scala
  val a = callServiceA()
  val b = callServiceB(a)
  val c = callServiceC(b)
  (a, c)
```

This issue affects the usability of any monadic interface (Future, Option, Stitch, Hydrator (servo), etc.).
As an alternative, Scala provides for-comprehensions to reduce noise:

```scala
  for {
    a <- callServiceA()
    b <- callServiceB(a)
    c <- callServiceC(b)
  } yield {
    (a, c)
  }
```

It’s a great tool to express sequential compositions and makes it easy to access the results of 
each composition step in the following steps. It also provides other features like local values, 
filtering, and identifier shadowing.


## Solution

Most mainstream languages have support for asynchronous programming using the async/await 
idiom or are implementing it (e.g. Scala, F#, C#/VB, Javascript, Python, Swift). Although useful, 
async/await is usually tied to a particular monad that represents asynchronous computations 
(Task, Future, etc.).

This design proposes a solution similar to async/await but generalized to any monad type through
a macro transformation. This generalization is an important factor considering that some codebases 
use monads in addition to Future for asynchronous computation. It’s likely that other monads
defined by the servo library would also benefit from this solution.

Given a monad `M`, the generalization uses the concept of lifting regular values to a monad
`T => M[T]` and unlifting values from a monad instance `M[T] => T`. Method signatures:

```scala
/**
 * Lifts the body to an instance of the monad `M`.
 * The implementation is a macro that automatically
 * transforms `unlift` calls into a regular monad
 * composition.
 */
def lift[T](body: T): M[T]

/**
 * Unlifts a value from a monad instance. This method
 * is valid only within a `lift` block. The `lift` macro
 * will replace calls to this method with a regular monad
 * composition.
 * 
 * @compileTimeOnly makes invocations outside a `lift`
 * block fail to compile.
 */
@compileTimeOnly("`unlift` must be used within `lift`")
def unlift[T](m: M[T]): T 
```

Example usage:

```scala
lift {
  val a = unlift(callServiceA())
  val b = unlift(callServiceB(a))
  val c = unlift(callServiceC(b))
  (a, c)
}
```

Note that `lift` corresponds to `async` and `unlift` to `await`.

## Usage

In order to use monadless with a given monad within Scala, one first must 
create the class which extends the `Monadless` trait and implement the following
'ghost' functions:

```scala
def apply[T](v: => T): M[T]
def join[T1, T2](m1: M[T1], m2: M[T2]): M[(T1, T2)]
def get[T](m: M[T]): T
def handle[T](m: M[T])(pf: PartialFunction[Throwable, T]): M[T]
def rescue[T](m: M[T])(pf: PartialFunction[Throwable, M[T]]): M[T]
def ensure[T](m: M[T])(f: => Unit): M[T]
```

For example, to begin using Monadless with a Future, implement `MonadlessFuture`:

```scala
class MonadlessFuture extends Monadless[Future] {
  def apply[T](v: => T)(implicit ec: ExecutionContext): Future[T] = Future(v)

  def join[T1, T2](m1: Future[T1], m2: Future[T2]): Future[(T1, T2)] = m1.zip(m2)

  def get[T](m: Future[T]): T = Await.result(m, 1.seconds)

  def handle[T](m: Future[T])(pf: PartialFunction[Throwable, T])(implicit ec: ExecutionContext): Future[T] = m.recover(pf)

  def rescue[T](m: Future[T])(pf: PartialFunction[Throwable, Future[T]])(implicit ec: ExecutionContext): Future[T] = m.recoverWith(pf)

  def ensure[T](m: Future[T])(f: => Unit)(implicit ec: ExecutionContext): Unit = m.onComplete { _ => f }
}
```

Then simply ensure that the functions in the `MonadlessFuture` class are in scope:

```scala
val monadlessFuture = new MonadlessFuture
import monadlessFuture._
```

And write code!

```scala
def callServiceA(): Future[String]
def callServiceB(): Future[String]

val twoServices: Future[String] = lift {
  val serviceAString = unlift(callServiceA())
  val serviceBString = unlift(callServiceB())
  s"service A gave us $serviceAString and service B gave us $serviceBString"
}
```

