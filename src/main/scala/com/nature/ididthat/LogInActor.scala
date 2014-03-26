package com.nature.ididthat

import akka.actor.Actor
import scala.util.{Failure, Success}
import LogInActor._

object LogInActor {
  case class LogIn(id: String, password: String)
  case class LogInSuccess(user: User)
  case class LogInFailure(e: Throwable)
}

/**
 * This actor should be very short lived as it is using blocking Ldap client.
 * One instance should be created for each login and then killed.
 */
class LogInActor(module: ConfigModule) extends Actor {
  val auth = module.authenticator

  override def receive = {
    case LogIn(id, pass) =>
      val result = auth.authenticate(id, pass)
      result match {
      case Success(user) => sender ! LogInSuccess(user)
      case Failure(e) => sender ! LogInFailure(e)
      case _ => sender ! LogInFailure(new IllegalArgumentException("The application should never get here"))
    }
  }
}
