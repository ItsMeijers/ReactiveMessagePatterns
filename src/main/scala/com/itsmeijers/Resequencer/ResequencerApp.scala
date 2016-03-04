package com.itsmeijers.Resequencer

import com.itsmeijers.CompletableApp
import akka.actor._
import java.util.concurrent.TimeUnit
import java.util.Date
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._
import ExecutionContext.Implicits.global

class ResequencerApp extends CompletableApp(10) {
   val sequencedMessageConsumer = system.actorOf(
      Props(classOf[SequencedMessageConsumer], this), "sequencedMEssageConsumer")

   val resequencerConsumer = system.actorOf(
      Props(classOf[ResequencerConsumer], sequencedMessageConsumer), "resequencerConsumer")

   val chaosRouter =
      system.actorOf(Props(classOf[ChaosRouter], resequencerConsumer), "chaosRouter")

   List.range(1, 6) foreach { n =>
      chaosRouter ! SequencedMessage("ABC", n, 5)
   }

   List.range(1, 6) foreach { n =>
      chaosRouter ! SequencedMessage("XYZ", n, 5)
   }

   this.awaitCompletion()
   println("Resequencer: is completed.")
}

case class SequencedMessage(
   correlationId: String,
   index: Int,
   total: Int)

case class ResequencedMessages(dispatchableIndex: Int, sequencedMessages: Array[SequencedMessage]) {
   def advancedTo(dispatchableIndex: Int) =
      ResequencedMessages(dispatchableIndex, sequencedMessages)
}

class ChaosRouter(consumer: ActorRef) extends Actor {
   val random = new Random((new Date()).getTime)

   def receive = {
      case sequencedMessage: SequencedMessage =>
         val millis = random.nextInt(100) + 1
         println(s"ChaosRouter: delaying delivere of $sequencedMessage for $millis ms.")

         val duration = Duration.create(millis, TimeUnit.MILLISECONDS)

         context.system.scheduler.scheduleOnce(duration, consumer, sequencedMessage)

      case message: Any =>
         println(s"ChaosRouter: unexpted: $message")
   }
}

class ResequencerConsumer(actualConsumer: ActorRef) extends Actor {

   val resequenced = scala.collection.mutable.Map[String, ResequencedMessages]()

   def dispatchAllSequenced(correlationId: String) = {
      val resequenceMessages = resequenced(correlationId)
      var dispatchableIndex = resequenceMessages.dispatchableIndex

      resequenceMessages.sequencedMessages.foreach { sequencedMessage =>
         if(sequencedMessage.index == dispatchableIndex) {
            actualConsumer ! sequencedMessage
            dispatchableIndex += 1
         }
      }

      resequenced(correlationId) = resequenceMessages.advancedTo(dispatchableIndex)
   }

   def dummySequencedMessages(count: Int): Seq[SequencedMessage] =
      List.fill(count)(SequencedMessage("", -1, count))

   def receive = {
      case unsequenceMessage: SequencedMessage =>
         println(s"ResequencerConsumer: received: $unsequenceMessage")
         resequence(unsequenceMessage)
         dispatchAllSequenced(unsequenceMessage.correlationId)
         removeCompleted(unsequenceMessage.correlationId)
      case msg: Any =>
         println(s"ResequencerConsumer: unexpected $msg")
   }

   def removeCompleted(correlationId: String) = {
      val resequencedMessages = resequenced(correlationId)

      if(resequencedMessages.dispatchableIndex >
            resequencedMessages.sequencedMessages(0).total) {
         resequenced.remove(correlationId)
         println(s"ResequencerConsumer: removed completed: $correlationId")
      }
   }

   def resequence(sequencedMessage: SequencedMessage) = {
      if (!resequenced.contains(sequencedMessage.correlationId)) {
         resequenced(sequencedMessage.correlationId) =
            ResequencedMessages(1, dummySequencedMessages(sequencedMessage.total).toArray)
      }

      resequenced(sequencedMessage.correlationId)
         .sequencedMessages
         .update(sequencedMessage.index - 1, sequencedMessage)
   }
}

class SequencedMessageConsumer(driver: CompletableApp) extends Actor {
   def receive = {
      case sequencedMessage: SequencedMessage =>
         println(s"SequencedMessageConsumer received: $sequencedMessage")
         driver.completedStep()
      case msg: Any =>
         println(s"SequencedMessageConsumer: unexpected: $msg")
   }
}
