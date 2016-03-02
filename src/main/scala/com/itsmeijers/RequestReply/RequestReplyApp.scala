package com.itsmeijers.RequestReply

import com.itsmeijers.CompletableApp
import akka.actor._

class RequestReplyApp extends CompletableApp(1) {
  val client = system.actorOf(Props(classOf[Client], this))
  val server = system.actorOf(Props(classOf[Server], this))

  client ! StartWith(server)

  this.awaitCompletion()
  println("Request Reply has finished!")
}

case class Request(what: String)
case class Reply(what: String)
case class StartWith(server: ActorRef)

class Client(driver: CompletableApp) extends Actor {
  def receive = {
    case StartWith(server) =>
      println("Client: is starting...")
      server ! Request("REQ-1")
    case Reply(what) =>
      println(s"Client: got reply: $what"); driver.completedStep()
  }
}

class Server(driver: CompletableApp) extends Actor {
  def receive = {
    case Request(what) =>
      println(s"Server: Got request: $what")
      sender() ! Reply(s"Response for: $what")
  }
}
