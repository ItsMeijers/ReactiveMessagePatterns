package com.itsmeijers.RecipientList

import com.itsmeijers.CompletableApp
import akka.actor._
import scala.collection.immutable.Seq

class RecipientListApp extends CompletableApp(5){
   def calculateDiscountPercentage(priceWithPercentage: Map[Double, Double], default: Double) =
      (orderTotalRetailPrice: Double) => {
         priceWithPercentage.find { case (low: Double, _: Double) =>
            orderTotalRetailPrice <= low
         }.map(_._2).getOrElse(default)
      }


   val orderProcessor = system.actorOf(Props[MontaineeringSuppliesOrderProcessor], "orderProcessor")

   val calculateDiscountBudgetHikers = calculateDiscountPercentage(
      Map(100.00 -> 0.02, 399.99 -> 0.03, 499.99 -> 0.05, 799.99 -> 0.07), 0.075
   )

   system.actorOf(
      Props(classOf[CompanyPriceQuotes], orderProcessor, 1.00, 1000.00, calculateDiscountBudgetHikers),
      "budgetHikers"
   )

   val calculateHighSierrie = calculateDiscountPercentage(
      Map(150.0 -> 0.015, 499.99 -> 0.02, 999.99 -> 0.03, 4999.99 -> 0.04), 0.05
   )

   system.actorOf(
      Props(classOf[CompanyPriceQuotes], orderProcessor, 100.00, 10000.00, calculateHighSierrie),
      "HighSierrie"
   )

   val calculateMountainAscent = calculateDiscountPercentage(
      Map(99.99 -> 0.01, 199.99 -> 0.02, 499.99 -> 0.03, 799.99 -> 0.04, 999.99 -> 0.045, 2999.99 -> 0.0475),
      0.05
   )

   system.actorOf(
      Props(classOf[CompanyPriceQuotes], orderProcessor, 100.00, 10000.00, calculateMountainAscent),
      "mountainAscent"
   )

   val calculatePinnacleGear = calculateDiscountPercentage(
      Map(299.99 -> 0.015, 399.99 -> 0.0175, 499.99 -> 0.02, 999.99 -> 0.03, 1199.99 -> 0.035, 4999.99 -> 0.04, 7999.99 -> 0.05),
      0.06
   )

   system.actorOf(
      Props(classOf[CompanyPriceQuotes], orderProcessor, 100.00, 10000.00, calculatePinnacleGear),
      "pinnacleGear"
   )

   val calculateRockBottomOuterwear = calculateDiscountPercentage(
      Map(100.00 -> 0.015, 399.99 -> 0.02, 499.99 -> 0.03, 799.99 -> 0.04, 999.99 -> 0.05,
          2999.99 -> 0.06, 4999.99 -> 0.07, 5999.99 -> 0.075),
      0.08
   )

   system.actorOf(
      Props(classOf[CompanyPriceQuotes], orderProcessor, 100.00, 10000.00, calculateRockBottomOuterwear),
      "rockBottomOuterwear"
   )

   orderProcessor ! RequestForQuotation("123", Vector(
      RetailItem("1", 29.95),
      RetailItem("2", 99.95),
      RetailItem("3", 14.95)
   ))

   orderProcessor ! RequestForQuotation("125", Vector(
      RetailItem("4", 39.99),
      RetailItem("5", 199.95),
      RetailItem("6", 149.95),
      RetailItem("7", 724.99)
   ))

   orderProcessor ! RequestForQuotation("129", Vector(
      RetailItem("8", 119.99),
      RetailItem("9", 499.95),
      RetailItem("10", 519.00),
      RetailItem("11", 209.50)
   ))

   orderProcessor ! RequestForQuotation("135", Vector(
      RetailItem("12", 0.97),
      RetailItem("13", 9.50),
      RetailItem("14", 1.99)
   ))

   orderProcessor ! RequestForQuotation("140", Vector(
      RetailItem("15", 107.50),
      RetailItem("16", 9.50),
      RetailItem("17", 599.99),
      RetailItem("18", 249.95),
      RetailItem("19", 789.99)
   ))
}

case class RequestForQuotation(rfqId: String, retailItems: Seq[RetailItem]){
   val totalRetailPrice = retailItems.foldLeft(0.0)(_ + _.retailPrice)
}

case class RetailItem(itemId: String, retailPrice: Double)

case class PriceQuoteInterest(
   path: String,
   quoteProcessor: ActorRef,
   lowTotalRetail: Double,
   highTotalRetail: Double
)

case class RequestPriceQuote(
   rfqId: String,
   itemId: String,
   retailPrice: Double,
   orderTotalRetailPrice: Double
)

case class PriceQuote(
   rfqId: String,
   itemId: String,
   retailPrice: Double,
   discountPrice: Double
)

import scala.collection.mutable

class MontaineeringSuppliesOrderProcessor extends Actor {
   val interestRegistry = mutable.Map[String, PriceQuoteInterest]()

   def calculateRecipientList(rfq: RequestForQuotation): Iterable[ActorRef] =
      for {
         interest <- interestRegistry.values
         if (rfq.totalRetailPrice >= interest.lowTotalRetail)
         if (rfq.totalRetailPrice <= interest.highTotalRetail)
      } yield interest.quoteProcessor

   def dispatchTo(rfq: RequestForQuotation, recipientList: Iterable[ActorRef]) =
      recipientList.foreach { recipient =>
         rfq.retailItems.foreach { retailItem =>
            println(s"OrderProcessor: ${rfq.rfqId} item: ${retailItem.itemId} to: ${recipient.path}")
            recipient ! RequestPriceQuote(
                           rfq.rfqId,
                           retailItem.itemId,
                           retailItem.retailPrice,
                           rfq.totalRetailPrice
                        )
         }
      }

   def receive = {
      case interest: PriceQuoteInterest =>
         interestRegistry(interest.path) = interest
      case priceQuote: PriceQuote =>
         println(s"OrderProcessor: received: $priceQuote")
      case rfq: RequestForQuotation =>
         val recipientList = calculateRecipientList(rfq)
         dispatchTo(rfq, recipientList)
      case message =>
         println(s"OrderProcessor: unexpected: $message")
   }
}

class CompanyPriceQuotes(
      interestRegistrar: ActorRef,
      lowTotalRetail: Double,
      highTotalRetail: Double,
      discountPercentage: Double => Double) extends Actor {

   interestRegistrar ! PriceQuoteInterest(self.path.toString, self, lowTotalRetail, highTotalRetail)

   def receive = {
      case rpq: RequestPriceQuote =>
         val discount = discountPercentage(rpq.orderTotalRetailPrice) * rpq.retailPrice

         println(s"${self.path.name}: calculated discount: $discount")

         sender() ! PriceQuote(rpq.rfqId, rpq.itemId, rpq.retailPrice, rpq.retailPrice - discount)
      case msg =>
         println(s"BudgetHikersPriceQuotes: unexpected: $msg")
   }
}
