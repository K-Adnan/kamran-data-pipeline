package dataemitter

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import dataemitter.ExampleData._

import scala.concurrent.duration._

object DataEmitter extends App {

  implicit val system: ActorSystem = ActorSystem("data-emitter")
  implicit val mat: Materializer = Materializer(system)

  private val combinedSource =
    Source.zipN(Seq(timeSource, firstNamesSource, lastNamesSource, phoneNumberSource, transactionId, transactionAmountSource))

  private val transactionFlow: Flow[Seq[Any], Transaction, _] =
    Flow[Seq[Any]].map { s =>
      Transaction(
        s(0).asInstanceOf[Long],
        s(1).asInstanceOf[String],
        s(2).asInstanceOf[String],
        s(3).asInstanceOf[String],
        s(4).asInstanceOf[Long],
        s(5).asInstanceOf[Double]
      )
    }

  combinedSource.throttle(5, 1.second).via(transactionFlow).runWith(Sink.foreach(println))
}
