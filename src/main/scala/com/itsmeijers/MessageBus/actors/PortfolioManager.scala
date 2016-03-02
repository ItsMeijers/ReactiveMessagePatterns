package com.itsmeijers.MessageBus.actors

import akka.actor._
import com.itsmeijers.CompletableApp

class PortfolioManager(driver: CompletableApp, tradingBus: ActorRef) extends Actor {
   import com.itsmeijers.MessageBus.actors.TradingBus._

   val applicationId = self.path.name

   tradingBus ! RegisterNotificationInterest(applicationId, "BuyOrderExecuted", self)
   tradingBus ! RegisterNotificationInterest(applicationId, "SellOrderExecuted", self)

   def receive = {
      case executed: BuyOrderExecuted =>
         println(s"PortfolioManager: adding holding: $executed")
         driver.completedStep()
      case executed: SellOrderExecuted =>
         println(s"PortfolioManager: adjusting holding: $executed")
         driver.completedStep()
      case message: Any => println(s"PortfolioManager: errormessage: $message")
   }
}

object PortfolioManager {
   def props(driver: CompletableApp, tradingBus: ActorRef) =
      Props(classOf[PortfolioManager], driver, tradingBus)
}
