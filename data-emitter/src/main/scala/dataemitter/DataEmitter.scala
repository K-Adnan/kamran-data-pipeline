package dataemitter

import akka.NotUsed
import akka.actor.{ActorSystem, Status}
import io.circe.generic.auto._
import akka.http.scaladsl.Http
import spray.json._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling._
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.google.api.core.{ApiFutureCallback, ApiFutures}
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.{PubsubMessage, TopicName}
import dataemitter.ExampleData._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import spray.json.DefaultJsonProtocol.jsonFormat7

import java.io.FileInputStream
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

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

  object TransactionJsonProtocol extends DefaultJsonProtocol {
    implicit val transactionFormat: RootJsonFormat[Transaction] = jsonFormat7(Transaction)
  }

  private val pubsubFlow: Flow[Transaction, Try[String], NotUsed] = Flow[Transaction].mapAsync(1) { t =>
    import TransactionJsonProtocol._
    val pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(t.toJson.compactPrint)).build()
    val publisher = Publisher
      .newBuilder(TopicName.of("kamran-test-project", "user-purchase-transaction"))
      .setCredentialsProvider(() =>
        GoogleCredentials.fromStream(new FileInputStream("/Users/kad43/kamran-test-project-key.json"))
      )
      .build()

    toScalaFuture(publisher.publish(pubsubMessage)).map { messageId =>
      Success(s"Message published with ID: $messageId")
    }.recover { case ex =>
      Failure(new Exception(s"Failed to publish message:", ex))
    }
  }

  def toScalaFuture[T](apiFuture: com.google.api.core.ApiFuture[T]): Future[T] = {
    val promise = Promise[T]()
    ApiFutures.addCallback(
      apiFuture,
      new ApiFutureCallback[T] {
        override def onFailure(t: Throwable): Unit = promise.failure(t)
        override def onSuccess(result: T): Unit = promise.success(result)
      },
      Executors.newSingleThreadExecutor()
    )
    promise.future
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
    .throttle(3, 1.second)
    .via(transactionFlow)
    .via(currencyFlow)
    .via(pubsubFlow)
    .runWith(Sink.foreach(println))
}
