package com.itsmeijers.MessageRouter.actors

import com.itsmeijers.CompletableApp
import akka.actor._

class Processor(driver: CompletableApp) extends Actor {
   def receive = {
      case msg: String =>
         println(s"Processor: ${self.path.name} receive message: $msg")
         driver.completedStep()
   }
}

object Processor {
   def props(driver: CompletableApp) = Props(classOf[Processor], driver)
}
