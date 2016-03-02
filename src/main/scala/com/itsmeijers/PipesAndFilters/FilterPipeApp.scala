package com.itsmeijers.PipesAndFilters

import com.itsmeijers.CompletableApp
import com.itsmeijers.PipesAndFilters.actors._
import akka.actor._

class FilterPipeApp extends CompletableApp(9) {

   val orderText = "(encryption)(certificate)<order id='123'>...</order>"
   val rawOrderBytes = orderText.toCharArray.map(_.toByte)

   val filter5 = system.actorOf(
      OrderManagementSystem.props(this),
      "orderManagementSystem")

   val filter4 = system.actorOf(
      Deduplicator.props(filter5, this),
      "Duplicator")

   val filter3 = system.actorOf(
      Authenticator.props(filter4, this),
      "Authenticator")

   val filter2 = system.actorOf(
      Decrypter.props(filter3, this),
      "Decrypter")

   val filter1 = system.actorOf(
      OrderAcceptanceEndpoint.props(filter2, this),
      "OrderAcceptanceEndPoint")

   filter1 ! rawOrderBytes
   filter1 ! rawOrderBytes

   this.awaitCompletion()

   println("PipesAndFiltersDriver: is completed")

}
