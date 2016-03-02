package com.itsmeijers.InvalidMessageChannel.actors

import akka.actor._

class InvalidMessageChannel extends Actor with ActorLogging {
   import InvalidMessageChannel._

   def receive = {
      case InvalidMessage(sender, receiver, message) =>
         /**
          * Could be doing all kinds of cool stuff here like:
          * - Notify the original sender
          * - Save to persitence store and send to the message once the receiver is available
          * - Logging to an actual logging systems
          * - Warn a monitor system about wrongly delivered messages
          * etc...
         **/
         log.debug(s"InvalidMessage received from: $sender to: $receiver with message: $message")
   }
}

object InvalidMessageChannel {

   def props = Props[InvalidMessageChannel]

   case class InvalidMessage(sender: ActorRef, receiver: ActorRef, message: Any)

}
