package com.itsmeijers.PipesAndFilters.actors

import akka.actor._
import com.itsmeijers.CompletableApp

class OrderManagementSystem(driver: CompletableApp) extends Actor {
   def receive = {
      case ProcessIncomingOrder(orderInfo) =>
         val text = new String(orderInfo)
         println(s"OrderManagementSsytem: processing unique order: $text")
         driver.completedStep()
   }
}

object OrderManagementSystem {
   def props(driver: CompletableApp) =
      Props(classOf[OrderManagementSystem], driver)
}
