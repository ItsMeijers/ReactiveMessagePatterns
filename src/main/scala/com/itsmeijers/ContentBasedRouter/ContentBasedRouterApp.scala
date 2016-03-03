package com.itsmeijers.ContentBasedRouter

import com.itsmeijers.CompletableApp
import akka.actor._
import scala.collection.immutable.Map

class ContentBasedRouterApp extends CompletableApp(3) {
   val orderRouter = system.actorOf(Props(classOf[OrderRouter], this), "contentRouter")

   val orderItem1 = OrderItem("1", "TypeABC.4", "An item of type ABC.4", 29.99)
   val orderItem2 = OrderItem("2", "TypeABC.1", "An item of type ABC.1", 23.99)
   val orderItem3 = OrderItem("3", "TypeABC.9", "An item of type ABC.9", 10.99)

   val orderItemsOfTypeA = Map(
      orderItem1.itemType -> orderItem1,
      orderItem2.itemType -> orderItem2,
      orderItem3.itemType -> orderItem3
   )

   orderRouter ! OrderPlaced(Order("123", "TypeABC", orderItemsOfTypeA))

   val orderItem4 = OrderItem("4", "TypeXYZ.4", "An item of type XYZ.4", 29.99)
   val orderItem5 = OrderItem("5", "TypeXYZ.1", "An item of type XYZ.1", 23.99)
   val orderItem6 = OrderItem("6", "TypeXYZ.9", "An item of type XYZ.9", 10.99)
   val orderItem7 = OrderItem("7", "TypeXYZ.5", "An item of type XYZ.5", 20.12)

   val orderItemsOfTypeX = Map(
      orderItem4.itemType -> orderItem4,
      orderItem5.itemType -> orderItem5,
      orderItem6.itemType -> orderItem6,
      orderItem7.itemType -> orderItem7
   )

   orderRouter ! OrderPlaced(Order("124", "TypeXYZ", orderItemsOfTypeX))

   this.awaitCompletion()
   println("ContentBasedRouter app is completed!")
}

case class Order(id: String, orderType: String, orderItems: Map[String, OrderItem]) {
   override def toString = s"OrderItem($id, $orderType, $grandTotal)"

   val grandTotal = orderItems.values.foldRight(0.0)(_.price + _)
}

case class OrderItem(id: String, itemType: String, description: String, price: Double)

case class OrderPlaced(order: Order) // Command Message

class OrderRouter(driver: CompletableApp) extends Actor {
   val inventorySystemA = context.actorOf(Props(classOf[InventorySystemA], driver), "inventorySystemA")

   val inventorySystemX = context.actorOf(Props(classOf[InventorySystemX], driver), "inventorySystemX")

   def receive = {
      case orderPlaced: OrderPlaced =>
         orderPlaced.order.orderType match {
            case "TypeABC" =>
               println(s"OrderRouter: routing $orderPlaced")
               inventorySystemA ! orderPlaced
            case "TypeXYZ" =>
               println(s"OrderRouter: routing $orderPlaced")
               inventorySystemX ! orderPlaced
         }
         driver.completedStep()
      case m =>
         println("OrderRouter: unexpected message")
         context.system.deadLetters forward m
   }

}

class InventorySystemA(val driver: CompletableApp) extends Actor {
   def receive = {
      case OrderPlaced(order) =>
         println(s"InventorySystemA: handling $order")
         driver.completedStep()
      case m =>
         println("InventorySystemA: unexpected message")
         context.system.deadLetters forward m
   }
}

class InventorySystemX(val driver: CompletableApp) extends Actor {
   def receive = {
      case OrderPlaced(order) =>
         println(s"InventorySystemA: handling $order")
         driver.completedStep()
      case m =>
         println("InventorySystemX: unexpected message")
         context.system.deadLetters forward m
   }
}
