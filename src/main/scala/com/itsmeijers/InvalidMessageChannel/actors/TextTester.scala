package com.itsmeijers.InvalidMessageChannel.actors

import akka.actor._
import com.itsmeijers.InvalidMessageChannel.actors.InvalidMessageChannel.InvalidMessage
import com.itsmeijers.CompletableApp

class TextTester(driver: CompletableApp, invalidMessageChannel: ActorRef) extends Actor with ActorLogging {
   def receive = {
      case text: String => log.debug(s"Success: $text"); driver.completedStep()
      case invalid: Any => 
         invalidMessageChannel ! InvalidMessage(sender, self, invalid)
         driver.completedStep()
   }
}

object TextTester {
   def props(driver: CompletableApp, invalidMessageChannel: ActorRef) =
      Props(classOf[TextTester], driver, invalidMessageChannel)
}
