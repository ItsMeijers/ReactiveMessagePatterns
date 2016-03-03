package com.itsmeijers.Splitter

import com.itsmeijers.CompletableApp
import akka.actor._
import scala.collection.Map

class SplitterApp extends CompletableApp(4) {
   val orderRouter = system.actorOf(Props(classOf[OrderRouter], this), "orderRouter")

   val orderItem1 = OrderItem("1", "TypeA", "An item of type A.", 23.95)
   val orderItem2 = OrderItem("1", "TypeB", "An item of type B.", 99.95)
   val orderItem3 = OrderItem("1", "TypeC", "An item of type C.", 14.95)

   val orderItems = Map(
      orderItem1.itemType -> orderItem1,
      orderItem2.itemType -> orderItem2,
      orderItem3.itemType -> orderItem3
   )

   orderRouter ! OrderPlaced(Order(orderItems))

   this.awaitCompletion()

   println("Splitter: is completed.")
}

case class OrderItem(id: String, itemType: String, description: String, price: Double)

case class Order(orderItems: Map[String, OrderItem]) {
   val grandTotal = orderItems.values.foldRight(0.0)(_.price + _)

   override def toString = s"Order(Order Items: $orderItems Totaling: $grandTotal)"
}

case class OrderPlaced(order: Order)
case class TypeAItemOrdered(orderItem: OrderItem)
case class TypeBItemOrdered(orderItem: OrderItem)
case class TypeCItemOrdered(orderItem: OrderItem)

class OrderRouter(driver: CompletableApp) extends Actor {

   val orderItemTypeAProcessor = context.actorOf(Props(classOf[OrderItemTypeAProcessor], driver), "orderItemAProcessor")
   val orderItemTypeBProcessor = context.actorOf(Props(classOf[OrderItemBProcessor], driver), "orderItemBProcessor")
   val orderItemTypeCProcessor = context.actorOf(Props(classOf[OrderItemCProcessor], driver), "orderItemCProcessor")

   def receive = {
      case OrderPlaced(order) =>
         println(order)
         order.orderItems foreach { case (itemType, orderItem) =>
            println(s"OrderRouter: routing $itemType")
            itemType match {
               case "TypeA" =>
                  orderItemTypeAProcessor ! TypeAItemOrdered(orderItem)
               case "TypeB" =>
                  orderItemTypeBProcessor ! TypeBItemOrdered(orderItem)
               case "TypeC" =>
                  orderItemTypeCProcessor ! TypeCItemOrdered(orderItem)
            }
         }
         driver.completedStep()
      case _ => println("OrderRouter: Received weird message!")
   }
}

class OrderItemTypeAProcessor(driver: CompletableApp) extends Actor {
   def receive = {
      case TypeAItemOrdered(orderItem) =>
         println(s"OrderItemTypeAProcessor: handling $orderItem")
         driver.completedStep()
      case _ => println("OrderTypeAProcessor: unexpected")
   }
}

class OrderItemBProcessor(driver: CompletableApp) extends Actor {
   def receive = {
      case TypeBItemOrdered(orderItem) =>
         println(s"OrderItemBProcessor: handling $orderItem")
         driver.completedStep()
      case _ => println("OrderItemBProcessor: unexpected")
   }
}

class OrderItemCProcessor(driver: CompletableApp) extends Actor {
   def receive = {
      case TypeCItemOrdered(orderItem) =>
         println(s"OrderItemCProcessor: handling $orderItem")
         driver.completedStep()
      case _ => println("OrderItemCProcessor: unexpected")
   }
}
