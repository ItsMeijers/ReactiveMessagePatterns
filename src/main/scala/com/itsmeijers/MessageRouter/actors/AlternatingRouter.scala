package com.itsmeijers.MessageRouter.actors

import akka.actor._
import com.itsmeijers.CompletableApp

class AlternatingRouter(
   val processor1: ActorRef,
   val processor2: ActorRef,
   driver: CompletableApp) extends Actor {

      var alternate = 1

      def alternateProcessor(): ActorRef =
         if(alternate == 1) {
            alternate = 2
            processor1
         } else {
            alternate = 1
            processor2
         }

      def receive = {
         case msg: String =>
            val processor = alternateProcessor()
            println(s" AlterantingRouter: routing message to ${processor.path.name}")
            processor ! msg
            driver.completedStep()
      }

}

object AlternatingRouter {
   def props(p1: ActorRef, p2: ActorRef, driver: CompletableApp) =
      Props(classOf[AlternatingRouter], p1, p2, driver)
}
