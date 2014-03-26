package com.nature.ididthat

import akka.actor.Actor
import RoleActor._
import com.allanbank.mongodb.{Callback, MongoFactory}
import com.allanbank.mongodb.bson.builder.BuilderFactory

object RoleActor {
  case class Create(role:Role)
  case class Delete(id: String)
  case class Load(id: String)
  case object ListRoles

  case object CreateSuccess
  case object CreateFail
  case object DeleteSuccess
  case class LoadSuccess(role: Role)
  case class ListSuccess(roles: Set[Role])
}

class RoleActor(config: ConfigModule) extends Actor {
  override def receive: Actor.Receive = {
    case Create(role) =>
      val client = MongoFactory.createClient(config.mongoConfiguration)

      val document = BuilderFactory.start()
      val perms = document.add("id", role.id)
        .pushArray("permissions")
      for (perm <- role.permissions) perms.add(perm.toString)

      val roles = client.getDatabase(config.configuration.mongoDatabase).getCollection("roles")

      roles.insertAsync(new Callback[Integer]{
        override def callback(count: Integer) {
          sender ! CreateSuccess
        }
        override def exception(e: Throwable) {
          sender ! CreateFail
        }
      }, document)
    case Delete(id) =>
    case Load(id) =>
    case ListRoles =>
  }
}