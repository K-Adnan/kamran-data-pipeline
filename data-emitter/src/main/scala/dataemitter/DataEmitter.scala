package dataemitter

import ExampleData.{firstNamesSource, lastNamesSource}
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.duration._

object DataEmitter extends App {

  implicit val system: ActorSystem = ActorSystem("data-emitter")
  implicit val mat: Materializer = Materializer(system)

  private val nameCombiner: Source[String, _] = firstNamesSource.zip(lastNamesSource)
    .map {
      case (firstName, lastName) => s"$firstName $lastName"
    }.throttle(5, 1.second)

  nameCombiner.runWith(Sink.foreach(println))
}
