package com.itsmeijers.MessageBus.actors

import akka.actor._
import com.itsmeijers.CompletableApp

class StockTrader(driver: CompletableApp, tradingBus: ActorRef) extends Actor {
   import com.itsmeijers.MessageBus.actors.TradingBus._

   val applicationId = self.path.name

   tradingBus ! RegisterCommandHandler(applicationId, "ExecuteBuyOrder", self)

   tradingBus ! RegisterCommandHandler(applicationId, "ExecuteSellOrder", self)

   def receive = {
      case buy: ExecuteBuyOrder =>
         println(s"StockTrader: buying for $buy")
         tradingBus ! TradingNotification("BuyOrderExecuted",
            BuyOrderExecuted(buy.portfolioId, buy.symbol, buy.quantity, buy.price))

         driver.completedStep()
      case sell: ExecuteSellOrder =>
         println(s"StockTrader: selling for: $sell")
         tradingBus ! TradingNotification("SellOrderExecuted",
            SellOrderExecuted(sell.portfolioId, sell.symbol, sell.quantity, sell.price))

         driver.completedStep()
      case message: Any => println(s"StockTrader: errormessage: $message")
   }
}

object StockTrader {
   def props(driver: CompletableApp, tradingBus: ActorRef) =
      Props(classOf[StockTrader], driver, tradingBus)
}
