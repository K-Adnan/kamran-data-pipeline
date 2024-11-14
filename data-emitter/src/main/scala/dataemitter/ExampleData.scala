package dataemitter

import akka.stream.scaladsl.Source
import dataemitter.ExampleData.fileToSeq

import java.time.Duration
import scala.util.Random

object ExampleData {

  def firstNamesSource: Source[String, _] = {
    val firstNames = fileToSeq("firstnames")
    Source.repeat(()).map { _ =>
      fileToSeq("firstnames")(Random.nextInt(firstNames.size))
    }
  }

  def lastNamesSource: Source[String, _] = {
    val lastNames = fileToSeq("lastnames")
    Source.repeat(()).map { _ =>
      lastNames(Random.nextInt(lastNames.size))
    }
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
    roundAmount(Random.between(1.99, 200))
  }

  def currencySource: Source[String, _] = {
    val currencies = fileToSeq("currencies")
    Source.repeat(()).map { _ =>
      currencies(Random.nextInt(currencies.size))
    }
  }

  private def fileToSeq(path: String): Seq[String] = {
    val source = scala.io.Source.fromResource(path)
    val seq = source.getLines().toSeq
    source.close
    seq
  }

  def roundAmount(amount: Double): Double = BigDecimal(amount).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  case class Transaction(
      transactionTime: Long,
      firstName: String,
      lastName: String,
      phoneNumber: String,
      transactionId: Long,
      transactionAmount: Double,
      currency: String
  )

  case class ExchangeRates(date: String, usd: Map[String, Double])
}
