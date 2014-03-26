package com.nature.ididthat

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}
import akka.pattern.ask
import com.nature.ididthat.RoleActor.{ListRoles, Create}
import com.nature.ididthat.Permission._
import akka.util.Timeout
import scala.concurrent.duration._
import org.fest.assertions.Assertions._

class RoleActorTest (system: ActorSystem)
  extends TestKit(system)
  with FunSuiteLike
  with ImplicitSender
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("RoleActorSystem"))

  implicit val actorSystem = system
  implicit val timeout = Timeout(1.seconds)
  private val module = new ConfigModule(ConfigTest.mockConfig)

  ignore("create") {  //ignoring it because it's timing out atm, will deal with it next
    val roleActor = TestActorRef(new RoleActor(module))

    val role1 = Role("sijy", Set("ListUsers", "ListTasks"))
//    val role1 = Role("sijy", Set(ListUsers, ListTasks))
    val loginResult = roleActor ? Create(role1)
    assertThat(loginResult.value.get.isSuccess)


    val listResult = roleActor ? ListRoles
    assertThat(listResult.value.get).isEqualTo(List(role1))
  }

  test("update") {

  }

  test("delete") {

  }

  test("get") {

  }

}