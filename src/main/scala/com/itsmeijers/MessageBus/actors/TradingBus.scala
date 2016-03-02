package com.itsmeijers.MessageBus.actors

import akka.actor._
import com.itsmeijers.CompletableApp
import scala.collection.mutable.Map

class TradingBus(driver: CompletableApp, canStartAfterRegistered: Int)
   extends Actor with ActorLogging {

      import TradingBus._

      val commandHandlers = Map[String, Vector[CommandHandler]]()
      val notificationInterests = Map[String, Vector[NotificationInterest]]()

      var totalRegistered = 0

      def dispatchCommand(command: TradingCommand) =
         if (commandHandlers.contains(command.commandId))
            commandHandlers(command.commandId) map (_.handler ! command.command)

      def dispatchNotification(notification: TradingNotification) =
         if(notificationInterests.contains(notification.notificationId))
            notificationInterests(notification.notificationId) map (_.handler ! notification.notification)

      def notifyStartWhenReady() = {
         totalRegistered += 1

         if(totalRegistered == this.canStartAfterRegistered)
            println(s"TradingBus: is ready: $totalRegistered"); driver.canStartNow()
      }

      def receive = {
         case register: RegisterCommandHandler =>
            println(s"TradingBus: register: $register")
            registerCommandHandler(register.commandId, register.applicationId, register.handler)
            notifyStartWhenReady()
         case register: RegisterNotificationInterest =>
            println(s"TradingBus: registering: $register")
            registerNotificationInterest(register.notificationId, register.applicationId, register.interested)
            notifyStartWhenReady()
         case command: TradingCommand =>
            println(s"TradingBus: dispatching: $command")
            dispatchCommand(command)
         case notification: TradingNotification =>
            println(s"TradingBus: dispatching: $notification")
            dispatchNotification(notification)
         case Status =>
            println(s"TradingBus: STATUS: $commandHandlers")
            println(s"TradingBus: STATUS: $notificationInterests")
         case message => println(s"TradingBus: received unexpected: $message")
      }


      def registerCommandHandler(commandId: String, applicationId: String, handler: ActorRef) = {
         if (!commandHandlers.contains(commandId))
            commandHandlers(commandId) = Vector[CommandHandler]()

         commandHandlers(commandId) =
            commandHandlers(commandId) :+ CommandHandler(applicationId, handler)
      }

      def registerNotificationInterest(notificationId: String, applicationId: String, interested: ActorRef) = {
         if(!notificationInterests.contains(notificationId))
            notificationInterests(notificationId) = Vector[NotificationInterest]()

         notificationInterests(notificationId) =
            notificationInterests(notificationId) :+ NotificationInterest(applicationId, interested)
      }

}

object TradingBus {

   def props(driver: CompletableApp, canStartAfterRegistered: Int) =
      Props(classOf[TradingBus], driver, canStartAfterRegistered)

   case class Money(amount: BigDecimal) {
      def this(amount: String) = this(new java.math.BigDecimal(amount))

      amount.setScale(4, BigDecimal.RoundingMode.HALF_UP)
   }

   case class CommandHandler(applicationId: String, handler: ActorRef)

   case class ExecuteBuyOrder(portfolioId: String, symbol: String, quantity: Int, price: Money)

   case class BuyOrderExecuted(portfolioId: String, symbol: String, quantity: Int, price: Money)

   case class ExecuteSellOrder(portfolioId: String, symbol: String, quantity: Int, price: Money)

   case class SellOrderExecuted(portfolioId: String, symbol: String, quantity: Int, price: Money)

   case class NotificationInterest(applicationId: String, handler: ActorRef)

   case class RegisterCommandHandler(applicationId: String, commandId: String, handler: ActorRef)

   case class RegisterNotificationInterest(applicationId: String, notificationId: String, interested: ActorRef)

   case class TradingCommand(commandId: String, command: Any)

   case class TradingNotification(notificationId: String, notification: Any)

   case object Status
}
