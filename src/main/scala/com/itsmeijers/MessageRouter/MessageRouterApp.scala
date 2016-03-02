package com.itsmeijers.MessageRouter

import com.itsmeijers.CompletableApp
import com.itsmeijers.MessageRouter.actors._

class MessageRouterApp extends CompletableApp(20) {
   val processor1 = system.actorOf(Processor.props(this), "processor1")
   val processor2 = system.actorOf(Processor.props(this), "processor2")

   val alternatingRouter = system.actorOf(
      AlternatingRouter.props(processor1, processor2, this),
      "alternatingRouter")

   for (count <- 1 to 10) {
      alternatingRouter ! "Message #" + count
   }

   awaitCompletion()

   println(s"MessageRouter: is completed")

}
