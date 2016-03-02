package com.itsmeijers.PipesAndFilters.actors

import akka.actor._
import com.itsmeijers.CompletableApp

case class ProcessIncomingOrder(rawOrder: Array[Byte])

class Decrypter(nextFilter: ActorRef, driver: CompletableApp) extends Actor {
   def receive = {
      case ProcessIncomingOrder(orderInfo) =>
         val text = new String(orderInfo)
         println(s"Decrypter: processing $text.")
         val orderText = text.replace("(encryption)", "")
         nextFilter ! ProcessIncomingOrder(orderText.toCharArray.map(_.toByte))
         driver.completedStep()
   }
}

object Decrypter {
   def props(nextFiler: ActorRef, driver: CompletableApp) =
      Props(classOf[Decrypter], nextFiler, driver)
}

class Authenticator(nextFilter: ActorRef, driver: CompletableApp) extends Actor {
   def receive = {
      case ProcessIncomingOrder(orderInfo) =>
         val text = new String(orderInfo)
         println(s"Authenticator: processing $text.")
         val orderText = text.replace("(certificate)", "")
         nextFilter ! ProcessIncomingOrder(orderText.toCharArray().map(_.toByte))
         driver.completedStep()
   }
}

object Authenticator {
   def props(nextFiler: ActorRef, driver: CompletableApp) =
      Props(classOf[Authenticator], nextFiler, driver)
}

class Deduplicator(nextFilter: ActorRef, driver: CompletableApp) extends Actor {
   val processedOrderIds = scala.collection.mutable.Set[String]()

   def orderIdFrom(orderText: String): String = {
      val orderIndex = orderText.indexOf("id='") + 4
      val orderIdLastIndex = orderText.indexOf("'", orderIndex)
      orderText.substring(orderIndex, orderIdLastIndex)
   }

   def receive = {
      case message@ProcessIncomingOrder(orderInfo) =>
         val text = new String(orderInfo)
         println(s"Duplicator: processing: $text.")
         val orderId = orderIdFrom(text)

         if(processedOrderIds.add(orderId))
            nextFilter ! message
         else
            println(s"Duplicator: Found duplicate order $orderId")

         driver.completedStep()
   }
}

object Deduplicator {
   def props(nextFiler: ActorRef, driver: CompletableApp) =
      Props(classOf[Deduplicator], nextFiler, driver)
}
