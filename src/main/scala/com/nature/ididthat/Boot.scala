package com.nature.ididthat

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

object Boot extends App {
  val module = Config.load(args)

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("IDidThat-actor-system")

  // create and start our service actor
  val apiActor = system.actorOf(Props(classOf[ApiActor], module), "ApiActor")

  implicit val timeout = Timeout(2.seconds)

  IO(Http) ? Http.Bind(
    listener = apiActor,
    interface = "localhost",
    port = module.configuration.port
  )
}
