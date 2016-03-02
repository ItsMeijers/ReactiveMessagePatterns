package com.itsmeijers.PipesAndFilters.actors

import akka.actor._
import com.itsmeijers.PipesAndFilters.FilterPipeApp
import com.itsmeijers.CompletableApp

class OrderAcceptanceEndpoint(nextFilter: ActorRef, driver: CompletableApp) extends Actor {
   def receive: Receive = {
      case rawOrder: Array[Byte] =>
         val text = new String(rawOrder)
         println(s"OrderAcceptanceEndpoint: processing $text")
         nextFilter ! ProcessIncomingOrder(rawOrder)
         driver.completedStep()
   }
}

object OrderAcceptanceEndpoint {
   def props(nextFilter: ActorRef, driver: CompletableApp) =
      Props(classOf[OrderAcceptanceEndpoint], nextFilter, driver)
}
