package dataemitter
import akka.stream.scaladsl.Source

import java.time.LocalDateTime

class TransactionGenerator {

//  def transactionSource: Source[Transaction, _] = ???

}

case class Transaction(transactionId: String, firstName: String, lastName: String, transactionTime: Long, transactionAmount: Double)
