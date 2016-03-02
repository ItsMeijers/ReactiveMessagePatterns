package com.itsmeijers.MessageExperation

import java.util.concurrent.TimeUnit
import java.util.Date
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._
import akka.actor._
import ExecutionContext.Implicits.global
import com.itsmeijers.CompletableApp

class MessageExperationApp extends CompletableApp(3) {
  val purchaseAgent =
    system.actorOf(Props(classOf[PurchaseAgent], this), "purchaseAgent")
  val purchaseRouter =
    system.actorOf(Props(classOf[PurchaseRouter], purchaseAgent), "purchaseRouter")

  purchaseRouter ! PlaceOrder("1", "11", 50.00, 1000)
  purchaseRouter ! PlaceOrder("2", "22", 250.00, 100)
  purchaseRouter ! PlaceOrder("3", "33", 32.95, 10)

  this.awaitCompletion()

  println("MessageExperation is completed.")
}

trait ExpiringMessage {
  val occurredOn = System.currentTimeMillis()
  val timeToLive: Long

  def isExpired: Boolean = System.currentTimeMillis() - occurredOn > timeToLive

}

case class PlaceOrder(
  id: String,
  itemId: String,
  price: Double,
  timeToLive: Long) extends ExpiringMessage

// Actor for simulating the delays from various causes
class PurchaseRouter(purchaseAgent: ActorRef) extends Actor {
  val random = new Random((new Date()).getTime)

  def receive = {
    case message =>
      val millis = random.nextInt(100) + 1
      println(s"PurchaseRouter: Delaying delivery of $message for $millis milliseconds")
      val duration = Duration.create(millis, TimeUnit.MILLISECONDS)

      context.system.scheduler.scheduleOnce(duration, purchaseAgent, message)
  }
}

class PurchaseAgent(driver: CompletableApp) extends Actor {
  def receive = {
    case placeOrder: PlaceOrder =>
      if(placeOrder.isExpired) {
        context.system.deadLetters ! placeOrder
        println(s"PurchaseAgent: delivered expired $placeOrder to dead letters")
      } else {
        println(s"PurchaseAgent: placing order for $placeOrder")
      }

      driver.completedStep()
  }
}
