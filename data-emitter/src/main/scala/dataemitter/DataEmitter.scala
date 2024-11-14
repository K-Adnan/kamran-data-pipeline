package dataemitter

import akka.actor.ActorSystem
import akka.actor.Status.{Failure, Success}
import io.circe.generic.auto._
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling._
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import dataemitter.ExampleData._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

object DataEmitter extends App {

  implicit val system: ActorSystem = ActorSystem("data-emitter")
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = mat.executionContext

  private val combinedSource =
    Source.zipN(
      Seq(
        timeSource,
        firstNamesSource,
        lastNamesSource,
        phoneNumberSource,
        transactionId,
        transactionAmountSource,
        currencySource
      )
    )

  private val transactionFlow: Flow[Seq[Any], Transaction, _] =
    Flow[Seq[Any]].map { s =>
      Transaction(
        s(0).asInstanceOf[Long],
        s(1).asInstanceOf[String],
        s(2).asInstanceOf[String],
        s(3).asInstanceOf[String],
        s(4).asInstanceOf[Long],
        s(5).asInstanceOf[Double],
        s(6).asInstanceOf[String]
      )
    }

  private val currencyFlow: Flow[Transaction, Transaction, _] =
    Flow[Transaction].mapAsync(1) { t =>
      convertToUSD(t.transactionAmount, t.currency).map { result =>
        t.copy(transactionAmount = result._1, currency = result._2)
      }.map {
        case t: Transaction => t
        case _              => Transaction(2L, "", "", "", 3L, 3.4, "")
      }
    }

  private def convertToUSD(amount: Double, cur: String)(implicit ec: ExecutionContext): Future[(Double, String)] = {
    val httpResponse = Http().singleRequest(
      HttpRequest(uri = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json")
    )

    val response = Await.result(httpResponse, 5.seconds)

    Unmarshal(response.entity)
      .to[ExchangeRates]
      .map { case c =>
        (roundAmount(c.usd(cur.toLowerCase)), "USD")
      }
      .recover { case _ =>
        (amount, cur)
      }
  }

  combinedSource
    .throttle(5, 1.second)
    .via(transactionFlow)
    .via(currencyFlow)
    .runWith(Sink.foreach(println))
}
