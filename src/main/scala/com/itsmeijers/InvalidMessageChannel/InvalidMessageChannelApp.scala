package com.itsmeijers.InvalidMessageChannel

import com.itsmeijers.CompletableApp
import com.itsmeijers.InvalidMessageChannel.actors._

class InvalidMessageChannelApp extends CompletableApp(4) {

   val invalidMessageChannel = system.actorOf(InvalidMessageChannel.props, "invalidMessageChannel")

   val textTester = system.actorOf(TextTester.props(this, invalidMessageChannel), "testTexter")

   textTester ! "First test message"

   textTester ! "Second test message"

   case class FakeMessage(int: Int, text: String, message: String)

   textTester ! FakeMessage(1, "Third message", "Fake!!!")

   textTester ! "Fourth test message"

   this.awaitCompletion()

   println("InvalidMessageChannelApp: is completed")

}
