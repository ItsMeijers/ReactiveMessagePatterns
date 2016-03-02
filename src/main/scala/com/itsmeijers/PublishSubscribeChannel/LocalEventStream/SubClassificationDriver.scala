package com.itsmeijers.PublishSubscribeChannel.LocalEventStream

import com.itsmeijers.CompletableApp
import akka.actor._
import akka.event.{EventBus, SubchannelClassification}
import akka.util.Subclassification


class SubClassificationDriver extends CompletableApp(6) {
   // Create a ActorRef thats going to subscribe to all the market messages
   val allSubscriber = system.actorOf(AllMarketSubscriber.props(this), "AllMarketSubscriber")

   // Create a ActorRef thats going to subscribe only to NASDAQ messages
   val nasdaqSubscriber = system.actorOf(NASDAQSubscriber.props(this), "NasdaqSubscriber")

   // Create a ActorRef thats going to subscribe only to NYSE messages
   val nyseSubscriber = system.actorOf(NYSESubscriber.props(this), "NYSESubscriber")

   // The quotesBus that manages the publish and subscribe messages
   val quotesBus = new QuotesEventBus

   // Subscribe the actors to their specific market message
   quotesBus.subscribe(allSubscriber, Market("quotes"))
   quotesBus.subscribe(nasdaqSubscriber, Market("quotes/NASDAQ"))
   quotesBus.subscribe(nyseSubscriber, Market("quotes/NYSE"))

   // Publish a few messages to the quoteBus which will send it to the designated actor
   quotesBus.publish(PricedQuote(Market("quotes/NYSE"), Symbol("ORCL"), new Money("37.84")))
   quotesBus.publish(PricedQuote(Market("quotes/NASDAQ"), Symbol("MSFT"), new Money("37.16")))
   quotesBus.publish(PricedQuote(Market("quotes/DAX"), Symbol("SAP:GR"), new Money("61.95")))
   quotesBus.publish(PricedQuote(Market("quotes/NKY"), Symbol("6701:JP"), new Money("237")))

   // Wait until the steps are done and then the system will shutdown
   awaitCompletion()

}

case class Money(amount: BigDecimal) {
   def this(amount: String) = this(new java.math.BigDecimal(amount))

   amount.setScale(4, BigDecimal.RoundingMode.HALF_UP)
}

case class Market(name: String)

// The message thats being send to the subscribers
case class PricedQuote(
   market: Market,
   ticker: Symbol,
   price: Money
)

class QuotesEventBus extends EventBus with SubchannelClassification {
   // Classifier is the type on which the comparrision gets made for the subscribers (see isEqual)
   type Classifier = Market
   // Type of event the bus listens to in this case PriceQuotes
   type Event = PricedQuote
   // Type of the listener (subscriber) to the EventBus
   type Subscriber = ActorRef

   // Get the Classifier from the Event
   protected def classify(event: Event): Classifier = {
      event.market
   }

   // Publish the event to the subscriber
   protected def publish(event: Event, subscriber: Subscriber): Unit = {
      subscriber ! event
   }

   // Create the methods to check how to compare messages to subscribers
   protected def subclassification = new Subclassification[Classifier] {

      def isEqual(
         subscribedToClassifier: Classifier,
         eventClassifier: Classifier): Boolean = {
         eventClassifier == subscribedToClassifier
      }

      def isSubclass(
         subscribedToClassifier: Classifier,
         eventClassifier: Classifier
      ): Boolean = {
         subscribedToClassifier.name.startsWith(eventClassifier.name)
      }

   }
}

// Actor that receives all the market messages
class AllMarketSubscriber(driver: CompletableApp) extends Actor {
   def receive = {
      case quote: PricedQuote =>
         println(s"AllMarketSubscriber received: $quote")
         driver.completedStep()
   }
}

object AllMarketSubscriber {
   def props(driver: CompletableApp) = Props(classOf[AllMarketSubscriber], driver)
}

// Actor that receives all the NASDAQ messages
class NASDAQSubscriber(driver: CompletableApp) extends Actor {
   def receive = {
      case quote: PricedQuote =>
         println(s"NASDAQSubscriber received: $quote")
         driver.completedStep()
   }
}

object NASDAQSubscriber {
   def props(driver: CompletableApp) = Props(classOf[NASDAQSubscriber], driver)
}

// Actor that receives all the NYSE messages
class NYSESubscriber(driver: CompletableApp) extends Actor {
   def receive = {
      case quote: PricedQuote =>
         println(s"NYSESubscriber received: $quote")
         driver.completedStep()
   }
}

object NYSESubscriber {
   def props(driver: CompletableApp) = Props(classOf[NYSESubscriber], driver)
}
