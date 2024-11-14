package dataemitter

import akka.stream.scaladsl.Source

import java.time.Duration
import scala.util.Random

object ExampleData {

  private val firstNames = fileToSeq("firstnames")
  private val lastNames = fileToSeq("lastnames")

  val firstNamesSource: Source[String, _] = Source.repeat(()).map { _ =>
    firstNames(Random.nextInt(firstNames.size))
  }

  val lastNamesSource: Source[String, _] = Source.repeat(()).map { _ =>
    lastNames(Random.nextInt(lastNames.size))
  }

  private val currentTimeMillis = System.currentTimeMillis()
  private val oneYearAgoMillis = currentTimeMillis - Duration.ofDays(365).toMillis

  val timeSource: Source[Long, _] = Source.repeat(()).map { _ =>
    oneYearAgoMillis + Random.nextLong(Duration.ofDays(365).toMillis)
  }

  val transactionId: Source[Long, _] = Source.repeat(()).map { _ =>
    Random.nextLong(99999999999L) + 20300000000000L
  }

  val phoneNumberSource: Source[String, _] = Source.repeat(()).map { _ =>
    "07" + Random.nextLong(999999999)
  }

  val transactionAmountSource: Source[Double, _] = Source.repeat(()).map { _ =>
    BigDecimal(Random.between(1.99, 200)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  private def fileToSeq(path: String): Seq[String] = {
    val source = scala.io.Source.fromResource(path)
    val seq = source.getLines().toSeq
    source.close
    seq
  }

  case class Transaction(
      transactionTime: Long,
      firstName: String,
      lastName: String,
      phoneNumber: String,
      transactionId: Long,
      transactionAmount: Double
  )
}
