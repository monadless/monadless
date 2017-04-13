package io.monadless

import java.net.{ ConnectException, UnknownHostException }
import play.api.libs.ws.{ WSRequest, WSResponse }
import play.api.libs.ws.ning.NingWSClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.async.Async.{ async, await }
import scala.collection.mutable
import scala.util.{ Failure, Success, Try }

trait ExampleHelper {
  val wsClient = NingWSClient()

  val goodRequest: WSRequest = wsClient.url("http://jsonplaceholder.typicode.com/comments/1")

  val badRequest: WSRequest = wsClient.url("http://not-a-good-domain.com")

  val timeOutRequest: WSRequest = wsClient.url("http://twitter.com:23842")

  def responseToString(wsResponse: WSResponse): String =
    if (!(200 to 299).contains(wsResponse.status)) {
      s"Received unexpected status ${wsResponse.status} : ${wsResponse.body}"
    } else {
      s"OK, received ${wsResponse.body}"
    }

  val monadlessFuture = new MonadlessFuture
  //  val monadlessTry = new MonadlessTry
}

object SimpleExample extends App with ExampleHelper {
  // Here's the normal way to do things
  val responseStringMap: Future[String] = goodRequest.get
    .flatMap(responseA =>
      goodRequest.get
        .map(responseB => responseToString(responseA) + responseToString(responseB)))

  println("responseStringMap: " + Await.result(responseStringMap, Duration.Inf))

  // Here's using scala async-await
  val responseStringAsyncAwait: Future[String] = async {
    responseToString(await(goodRequest.get)) + responseToString(await(goodRequest.get))
  }
  println("responseStringAsyncAwait: " + Await.result(responseStringAsyncAwait, Duration.Inf))

  // Here's using Monadless
  import monadlessFuture._

  val responseStringMonadless: Future[String] = lift {
    responseToString(unlift(goodRequest.get)) + responseToString(unlift(goodRequest.get))
  }
  println("responseStringMonadless: " + Await.result(responseStringMonadless, Duration.Inf))
}

object ExceptionalExample extends App with ExampleHelper {

  import monadlessFuture._

  // Monadless lets you deal with exceptional results
  // Async-await doesn't support this!
  val responseStringC: Future[String] = lift {
    try {
      responseToString(unlift(badRequest.get))
    } catch {
      case e: Exception => s"received an exceptional result: $e"
    }
  }
  println("responseStringC: " + Await.result(responseStringC, Duration.Inf))

  // Otherwise you'd write
  val responseStringD: Future[String] = badRequest
    .get
    .map(responseToString)
    .recover {
      case e: Exception => s"received an exceptional result: $e"
    }
  println("responseStringD: " + Await.result(responseStringD, Duration.Inf))

  // We can throw futures into the catch clause
  val catchExample: Future[String] = lift {
    val firstResponse = try {
      unlift(badRequest.get).body.toString
    } catch {
      case e: UnknownHostException => unlift(goodRequest.get).body.toString
    }
    val secondResponse = try {
      unlift(timeOutRequest.get).body.toString
    } catch {
      case e: ConnectException => s"bad response: $e"
    }
    firstResponse + secondResponse
  }
  println("catchExample: " + Await.result(catchExample, Duration.Inf))
}

object ControlExample extends App with ExampleHelper {

  import monadlessFuture._

  // We can do if clauses
  val ifClauses: Future[String] = lift {
    if (unlift(goodRequest.get).status == 200) {
      s"We got a good response"
    } else {
      s"We got a bad response"
    }
  }
  println("ifClauses: " + Await.result(ifClauses, Duration.Inf))

  // We can do cause clauses
  val caseClauses: Future[String] = lift {
    unlift(goodRequest.get).status match {
      case 200 =>
        try {
          unlift(badRequest.get).body.toString
        } catch {
          case e: UnknownHostException => unlift(goodRequest.get).body.toString
        }
      case _ => s"Got some other response"
    }
  }
  println("caseClauses: " + Await.result(caseClauses, Duration.Inf))

  // What if you want to implement a loop
  val listResults: Future[List[Int]] = lift {
    val mutableList: mutable.MutableList[Int] = mutable.MutableList()

    do {
      mutableList += unlift(goodRequest.get).status
    } while (mutableList.length < 10)
    mutableList.toList
  }
  println("listResults: " + Await.result(listResults, Duration.Inf))

  // Otherwise you'd have to use recursion and have code that's harder to follow
  def loop(l: List[Int]): Future[List[Int]] = l.length match {
    case 10 => Future.successful(l)
    case _ => goodRequest.get
      .map(_.status)
      .flatMap(status => loop(status :: l))
  }

  val listResultsB: Future[List[Int]] = loop(List())
  println("listResultsB: " + Await.result(listResultsB, Duration.Inf))
}

//object TryExample extends App with ExampleHelper {
//
//  import monadlessTry._
//
//  def confusedTokenizer(input: String): Try[List[String]] = {
//    input.split(",").toList match {
//      case _ :: Nil => Failure(new RuntimeException("string had no commas"))
//      case list => Success(list)
//    }
//  }
//
//  def tryExample(string: String): Try[List[String]] = lift {
//    unlift(confusedTokenizer(string)).map(element => element + "A")
//  }
//
//  println("tryExample1:" + tryExample("string with no commas"))
//  println("tryExample2:" + tryExample("string,with,commas"))
//}
//
