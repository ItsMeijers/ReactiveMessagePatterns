package com.itsmeijers.MessageBus.actors

import akka.actor._
import com.itsmeijers.CompletableApp

class MarketAnalysisTools(driver: CompletableApp, tradingBus: ActorRef) extends Actor {
   import com.itsmeijers.MessageBus.actors.TradingBus._

   val applicationId = self.path.name

   tradingBus ! RegisterNotificationInterest(applicationId, "BuyOrderExecuted", self)

   tradingBus ! RegisterNotificationInterest(applicationId, "SellOrderExecuted", self)

   def receive = {
      case executed: BuyOrderExecuted =>
         println(s"MarketAnalysisTools: adding: $executed")
         driver.completedStep()
      case executed: SellOrderExecuted =>
         println(s"MarketAnalysisTools: adjusting: $executed")
         driver.completedStep()
      case message: Any =>
         println(s"MarketAnalysisTools: unexpected: $message")
   }
}

object MarketAnalysisTools {
   def props(driver: CompletableApp, tradingBus: ActorRef) =
      Props(classOf[MarketAnalysisTools], driver, tradingBus)
}
