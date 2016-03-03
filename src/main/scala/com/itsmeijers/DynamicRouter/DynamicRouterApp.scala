package com.itsmeijers.DynamicRouter

import com.itsmeijers.CompletableApp
import akka.actor._
import scala.reflect.runtime.currentMirror

class DynamicRouterApp extends CompletableApp(5) {
   val dunnoInterested = system.actorOf(Props(classOf[DunnoInterest], this), "dunnoInterested")

   val typedMessageInterestRouter = system.actorOf(
      Props(classOf[TypedMessageInterestRouter], dunnoInterested, 4, 1, this),
      "typeMessageInterestRouter"
   )

   val typeAInterest = system.actorOf(
      Props(classOf[TypeAInterested], typedMessageInterestRouter, this),
      "typeAInterest"
   )

   val typeBInterest = system.actorOf(
      Props(classOf[TypeBInterested], typedMessageInterestRouter, this),
      "typeBInterest"
   )

   val typeCInterest = system.actorOf(
      Props(classOf[TypeCInterested], typedMessageInterestRouter, this),
      "typeCInterest"
   )

   val typeCAlsoInterested = system.actorOf(
      Props(classOf[TypeCAlsoInterested], typedMessageInterestRouter, this),
      "typeCAlsoInterested"
   )

   this.awaitCanStartNow()

   typedMessageInterestRouter ! TypeAMessage("Messsage of TypeA.")
   typedMessageInterestRouter ! TypeBMessage("Messsage of TypeB.")
   typedMessageInterestRouter ! TypeCMessage("Messsage of TypeC.")

   this.awaitCanStartNow()

   typedMessageInterestRouter ! TypeCMessage("Another message of TypeC.")
   typedMessageInterestRouter ! TypeDMessage("Message of typeD.")

   this.awaitCompletion()

   println("DynamicRouter: is completed.")
}

case class InterestedIn(messageType: String)
case class NoLongerInterestedIn(messageType: String)

case class TypeAMessage(description: String)
case class TypeBMessage(description: String)
case class TypeCMessage(description: String)
case class TypeDMessage(description: String)

class TypedMessageInterestRouter(
      dunnoInterested: ActorRef,
      canStartAfterRegistered: Int,
      canCompleteAfterUnregistered: Int,
      driver: CompletableApp) extends Actor {

   import collection.mutable
   val interestRegistry = mutable.Map[String, ActorRef]()
   val secondaryInterestRegistry = mutable.Map[String, ActorRef]()

   var unregisteredCount = 0

   def receive = {
      case interestedIn: InterestedIn =>
         registerInterest(interestedIn)
      case noLongerInterestedIn: NoLongerInterestedIn =>
         unregisterInterest(noLongerInterestedIn)
      case message => sendFor(message)
   }

   def registerInterest(interestedIn: InterestedIn) = {
      val messageType = typeOfMessage(interestedIn.messageType)

      if(!interestRegistry.contains(messageType))
         interestRegistry(messageType) = sender()
      else
         secondaryInterestRegistry(messageType) = sender()

      if(interestRegistry.size + secondaryInterestRegistry.size >= canStartAfterRegistered)
         driver.canStartNow()
   }

   def unregisterInterest(noLongerInterestedIn: NoLongerInterestedIn) = {
      val messageType = typeOfMessage(noLongerInterestedIn.messageType)

      if(interestRegistry.contains(messageType)){
         val wasInterested = interestRegistry(messageType)

         if(wasInterested.compareTo(sender) == 0) {
            if(secondaryInterestRegistry.contains(messageType)) {
               val nowInterested = secondaryInterestRegistry.remove(messageType)

               interestRegistry(messageType) = nowInterested.get
            } else {
               interestRegistry.remove(messageType)
            }

            unregisteredCount += 1

            if(unregisteredCount >= this.canCompleteAfterUnregistered)
               driver.canCompleteNow()
         }
      }
   }

   def sendFor(message: Any) = {
      val messageType =
         typeOfMessage(currentMirror.reflect(message).symbol.toString)
      if (interestRegistry.contains(messageType))
         interestRegistry(messageType) forward message
      else dunnoInterested ! message
   }

   def typeOfMessage(rawMessageType: String): String =
      rawMessageType.replace('$', ' ').replace('.', ' ').split(' ').last.trim

}

// Actor to send message when interst is not registered
class DunnoInterest(driver: CompletableApp) extends Actor {
   def receive = {
      case message =>
      println(s"DunnoInterest: receive undeliverable message $message")
      driver.completedStep()
   }
}

class TypeAInterested(interestRouter: ActorRef, driver: CompletableApp) extends Actor {
   // inform the interestRouter about the interest.
   interestRouter ! InterestedIn(TypeAMessage.getClass.getName)

   def receive = {
      case message: TypeAMessage =>
         println(s"TypeAInterested: received: $message")
         driver.completedStep()
      case message =>
         println(s"TypeAInterested: unexpected: $message")
   }
}

class TypeBInterested(interestRouter: ActorRef, driver: CompletableApp) extends Actor {
   // inform the interestRouter about the interest.
   interestRouter ! InterestedIn(TypeBMessage.getClass.getName)

   def receive = {
      case message: TypeBMessage =>
         println(s"TypeBInterested: received: $message")
         driver.completedStep()
      case message =>
         println(s"TypeBInterested: unexpected: $message")
   }
}

class TypeCInterested(interestRouter: ActorRef, driver: CompletableApp) extends Actor {
   // inform the interestRouter about the interest.
   val messageName = TypeCMessage.getClass.getName
   interestRouter ! InterestedIn(messageName)

   def receive = {
      case message: TypeCMessage =>
         println(s"TypeCInterested: received: $message")
         interestRouter ! NoLongerInterestedIn(messageName)
         driver.completedStep()
      case message =>
         println(s"TypeCInterested: unexpected: $message")
   }
}

class TypeCAlsoInterested(interestRouter: ActorRef, driver: CompletableApp) extends Actor {
   // inform the interestRouter about the interest.
   val messageName = TypeCMessage.getClass.getName
   interestRouter ! InterestedIn(messageName)

   def receive = {
      case message: TypeCMessage =>
         println(s"TypeCAlsoInterested: received: $message")
         interestRouter ! NoLongerInterestedIn(messageName)
         driver.completedStep()
      case message =>
         println(s"TypeCAlsoInterested: unexpected: $message")
   }
}
