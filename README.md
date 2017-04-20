![monadless](https://raw.githubusercontent.com/monadless/monadless/master/monadless.png)

Syntactic sugar for monad composition (or: "async/await" generalized)

[![Build Status](https://travis-ci.org/monadless/monadless.svg?branch=master)](https://travis-ci.org/monadless/monadless)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/ea4068928617433f8275534af3351152)](https://www.codacy.com/app/fwbrasil/monadless)
[![Join the chat at https://gitter.im/monadless/monadless](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/monadless/onadless?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)-[![Dependency Status](https://www.versioneye.com/user/projects/58f1b1915c12c800161e64d1/badge.svg?style=flat)](https://www.versioneye.com/user/projects/58f1b1915c12c800161e64d1)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.monadless/monadless_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.monadless/monadless_2.11)
[![Javadocs](https://www.javadoc.io/badge/io.monadless/monadless_2.11.svg)](https://www.javadoc.io/doc/io.monadless/monadless-core_2.11)

## Problem

Dealing with monad compositions involves considerable syntax noise. For instance, this code using the `Future` monad:

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

This issue affects the usability of any monadic interface (Future, Option, Try, etc.). As an alternative, Scala provides for-comprehensions to reduce the noise:

```scala
  for {
    a <- callServiceA()
    b <- callServiceB(a)
    c <- callServiceC(b)
  } yield {
    (a, c)
  }
```

They are useful to express sequential compositions and make it easy to access the results of each for-comprehension step from the following ones, but it doesn't provide syntax sugar for Scala constructs other than assignment (`<-`, `=`) and mapping (`yield`).

## Solution

Most mainstream languages have support for asynchronous programming using the async/await idiom or are implementing it (e.g. F#, C#/VB, Javascript, Python, Swift). Although useful, async/await is usually tied to a particular monad that represents asynchronous computations (`Task`, `Future`, etc.).

This library implements a solution similar to async/await but generalized to any monad type. This generalization is a major factor considering that some codebases use other monads like `Task` in addition to `Future` for asynchronous computations.

Given a monad `M`, the generalization uses the concept of lifting regular values to a monad (`T => M[T]`) and unlifting values from a monad instance (`M[T] => T`). Example usage:

```scala
lift {
  val a = unlift(callServiceA())
  val b = unlift(callServiceB(a))
  val c = unlift(callServiceC(b))
  (a, c)
}
```

Note that `lift` corresponds to `async` and `unlift` to `await`.

## Getting started

The `lift` and `unlift` methods are provided by an instance of `io.monadless.Monadless`. The library is generic and can be used with any monad type, but sub-modules with pre-defined `Monadless` instances are provided for convenience:

### `monadless-stdlib`

SBT configuration:
```
libraryDependencies += "io.monadless" %% "monadless-stdlib" % "0.0.11"
```

Imports:
```scala
// for `scala.concurrent.Future`
import io.monadless.stdlib.MonadlessFuture._

// for `scala.Option`
// note: doesn't support `try`/`catch`/`finally`
import io.monadless.stdlib.MonadlessOption._

// to use Monadless with `scala.util.Try`
import io.monadless.stdlib.MonadlessTry._
```

### `monadless-monix`

SBT configuration:
```
libraryDependencies += "io.monadless" %% "monadless-monix" % "0.0.11"
```

Usage:
```scala
// for `monix.eval.Task`
import io.monadless.monix.MonadlessTask._
```

### `monadless-monix`

SBT configuration:
```
libraryDependencies += "io.monadless" %% "monadless-monix" % "0.0.11"
```

Usage:
```scala
// for `monix.eval.Task`
import io.monadless.monix.MonadlessTask._
```

### `monadless-cats`

SBT configuration:
```
libraryDependencies += "io.monadless" %% "monadless-cats" % "0.0.11"
```

Usage:
```scala
// for `cats.Applicative`
// note: doesn't support `try`/`catch`/`finally`
val myApplicativeMonadless = io.monadless.cats.MonadlessApplicative[MyApplicative]()
import myApplicativeMonadless._

// for `cats.Monad`
// note: doesn't support `try`/`catch`/`finally`
val myMonadMonadless = io.monadless.cats.MonadlessMonad[MyMonad]()
import myMonadMonadless._
```

### `monadless-algebird`

SBT configuration:
```
libraryDependencies += "io.monadless" %% "monadless-algebird" % "0.0.11"
```

Usage:
```scala
// for `com.twitter.algebird.Applicative`
// note: doesn't support `try`/`catch`/`finally`
val myApplicativeMonadless = io.monadless.algebird.MonadlessApplicative[MyApplicative]()
import myApplicativeMonadless._

// for `com.twitter.algebird.Monad`
// note: doesn't support `try`/`catch`/`finally`
val myMonadMonadless = io.monadless.algebird.MonadlessMonad[MyMonad]()
import monadless._
```

### Twitter monads

The default method resolution uses the naming conventions adopted by Twitter, so it's possible to use the default `Monadless` for them:

```scala
val futureMonadless = io.monadless.Monadless[com.twitter.util.Future]()
import futureMonadless._

val tryMonadless = io.monadless.Monadless[com.twitter.util.Try]()
import tryMonadless
```

### Other monads

See ["How does it work?"](#how-does-it-work) for information on how to define a `Monadless` instance for other monads.

## Supported constructs

`val`s:
```scala
lift {
  val i = unlift(a)
  i + 1
}
```

nested blocks of code:
```scala
lift {
  val i = {
     val j = unlift(a)
     j * 3
  }
  i + 1
}
```

`val` pattern matching:
```scala
lift {
  val (i, j) = (unlift(a), unlift(b))
}
```

`if` conditions:
```scala
lift {
  if(unlift(a) == 1) unlift(c)
  else 0
}
```

`boolean` operations (including short-circuiting):
```scala
lift {
  unlift(a) == 1 || (unlift(b) == 2 && unlift(c) == 3)
}
```

`def`:
```scala
lift {
  def m(j: Int) = unlift(a) + j
  m(unlift(b))
}
```

recursive `def`s:
```scala
lift {
  def m(j: Int) = if(j == 0) unlift(a) else m(j - 1)
  m(10)
}
```

`trait`s, `class`es, and `objects:
```scala
lift {
  trait A {
    def i = unlift(a)
  }
  class B extends A {
    def j = i + 1
  }
  object C {
    val k = unlift(c)
  }
  (new B).j + C.k
}
```

pattern matching:
```scala
lift {
  unlift(a) match {
    case 1 => unlift(b)
    case _ => unlift(c)
  }
}
```

`try`/`catch`/`finally`:
```scala
lift {
  try unlift(a)
  catch {
    case e => unlift(b)
  } finally {
    println("done")
  }
}
```

`while` loops:
```scala
lift {
  var i = 0
  while(i < 10)
    i += unlift(a)
}
```

The [`UnsupportedSpec`](https://github.com/monadless/monadless/blob/master/monadless-core/src/test/scala/io/monadless/UnsupportedSpec.scala) lists the constructs that are known to be unsupported. Please report if you find a construct that can't be translated and is not classified by the spec class.

## How does it work?

The `unlift` method is only a marker that indicates that the `lift` macro transformation needs to translate a value into a monad instance. For example, it never blocks thread using `Await.result` if it's dealing with a `Future`. 

The code generated by the macro uses an approach similar to for-comprehensions, resolving at compile time the methods that are required for the composition and not requiring a particular monad interface. We call these "ghost" methods: they aren't defined by an interface and only need to be source-compatible with the generated macro tree. To elucidate, let's take `map` as an example:

```scala
// Option `map` signature
def map[B](f: A => B): Option[B]

// Future `map` signature
def map[B](f: A => B)(implicit ec: ExecutionContext)
```

`Future` and `Option` are supported by for-comprehensions and `lift` even though they don't share the same method signature since `Future` requires an `ExecutionContext`. They are only required to be source-compatible with the transformed tree. Example `lift` transformation:

```scala
def a: Future[Int] = ???

// this transformation
lift {
  unlift(a) + 1
}

// generates the tree
a.map(_ + 1)

// that triggers scala's implicit resolution after the
// macro transformation and becomes:
a.map(_ + 1)(theExecutionContext)
```

For-comprehensions use only two "ghost" methods: `map` and `flatMap`. To support more Scala constructs, Monadless requires more methods. This is the definition of the "ghost" interface that Monadless expects:

```scala

trait M[A] {
  
  // Applies the map function
  def map[B](f: A => B): M[B]

  // Applies `f` and then flattens the result
  def flatMap[B](f: A => M[B]): M[B]
  
  // Recovers from a failure if the partial function 
  // is defined for the failure. Used to translate `catch` clauses.
  def rescue(pf: PartialFunction[Throwable, M[A]]): M[A]

  // Executes `f` regarless of the outcome (success/failure).
  // Used to translate `finally` clauses.
  def ensure(f: => Unit): M[A]
}

object M {

  // Creates a monad instance with the result of `f`
  def apply[A](f: => T): M[A]

  // Transforms multiple monad instances into one.
  def collect[A](l: List[M[A]]): M[List[A]]
}

```

As an alternative to using the monad type methods directly since not all existing monads implement them, Monadless allows the user to define the implementations separately:

```scala
object CustomMonadless extends Monadless[M] {

  // these are also "ghost" methods
  def apply[A](f: => A): M[A] = ???
  def collect[A](l: List[M[A]]): M[List[A]] = ???
  def map[A, B](m: M[A])(f: A => B): M[B] = ???
  def flatMap[A, B](m: M[A])(f: A => M[B]): M[B] = ???
  def rescue[A](m: M[A])(pf: PartialFunction[Throwable, M[A]]): M[A] = ??
  def ensure[A](m: M[A])(f: => Unit): M[A] = ???
}
```

The methods defined by the `Monadless` instance have precedence over the ones specified by the monad instance and its companion object

Related projects
----------------

- [scala-async](https://github.com/scala/async) (for scala `Future`s)
- [effectful](https://github.com/pelotom/effectful) (for scalaz `Monad`s)
- [each](https://github.com/ThoughtWorksInc/each) (also for scalaz `Monad`s)


Code of Conduct
---------------

Please note that this project is released with a Contributor Code of Conduct. By participating in this project, you agree to abide by its terms. See [CODE_OF_CONDUCT.md](https://github.com/monadless/monadless/blob/master/CODE_OF_CONDUCT.md) for details.

License
-------

See the [LICENSE](https://github.com/monadless/monadless/blob/master/LICENSE.txt) file for details.

Maintainers
===========

- @fwbrasil
- @sameerparekh