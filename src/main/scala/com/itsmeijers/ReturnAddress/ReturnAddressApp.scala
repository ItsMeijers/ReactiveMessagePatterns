package com.itsmeijers.ReturnAddress

import com.itsmeijers.CompletableApp
import akka.actor._

class ReturnAddressApp extends CompletableApp(2) {

  val client = system.actorOf(Props(classOf[Client], this), "client")
  val server = system.actorOf(Props(classOf[Server]), "server")

  client ! StartWith(server)

  this.awaitCompletion()
  println("ReturnAddress App is finished")
}

case class Request(what: String)
case class ComplexRequest(what: String)
case class Reply(what: String)
case class ReplyToComplex(what: String)
case class StartWith(server: ActorRef)

class Client(driver: CompletableApp) extends Actor {
  def receive = {
    case StartWith(server) =>
      println("Client is starting...")
      server ! Request("REQ-1")
      server ! ComplexRequest("REQ-20")
    case Reply(what) =>
      println(s"Client received reply: $what")
      driver.completedStep()
    case ReplyToComplex(what) =>
      println(s"Client received complex reply: $what")
      driver.completedStep()
  }
}

class Server extends Actor {
  val worker = context.system.actorOf(Props(classOf[Worker]), "worker")

  def receive = {
    case Request(what) =>
      println(s"Server: received request: $what")
      sender() ! Reply(s"Reply to $what")
    case complexRequest: ComplexRequest =>
      println(s"Server: received request ${complexRequest.what}")
      worker forward complexRequest
  }
}

class Worker extends Actor {
  def receive = {
    case ComplexRequest(what) =>
      println(s"Worker: received request: $what")
      sender() ! ReplyToComplex(s"Reply to $what")
  }
}
