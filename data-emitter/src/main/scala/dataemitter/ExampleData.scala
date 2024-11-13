package dataemitter

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.util.Random

object ExampleData {

  private val firstNames = fileToSeq("firstnames")
  private val lastNames = fileToSeq("lastnames")

  val firstNamesSource: Source[String, _] = Source.repeat(()).map { _ =>
    firstNames(Random.nextInt(firstNames.size))
  }

  val lastNamesSource: Source[String, NotUsed] = Source.repeat(()).map { _ =>
    lastNames(Random.nextInt(lastNames.size))
  }

  private def fileToSeq(path: String): Seq[String] = {
    val source = scala.io.Source.fromResource(path)
    val seq = source.getLines().toSeq
    source.close
    seq
  }
}
