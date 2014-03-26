package com.nature.ididthat

import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSuiteLike}
import org.mockito.Mockito._
import scala.util.{Failure, Success}
import com.nature.ididthat.LogInActor._
import akka.pattern.ask
import org.fest.assertions.Assertions._
import scala.concurrent.duration._
import akka.util.Timeout
import com.github.simplyscala.{MongodProps, MongoEmbedDatabase}

class LogInActorTest(system: ActorSystem)
  extends TestKit(system)
  with FunSuiteLike
  with ImplicitSender
  with BeforeAndAfterAll
  with BeforeAndAfter
  with MongoEmbedDatabase {

  def this() = this(ActorSystem("LogInActorSystem"))
  val auth = mock(classOf[LdapAuthenticator])
  val user = mock(classOf[User])
  val loginFailureException = new Exception()
  val authenticateException = new RuntimeException()
  when(auth.authenticate("user1", "pass1")).thenReturn(Success(user))
  when(auth.authenticate("user2", "pass2")).thenReturn(Failure(loginFailureException))

  implicit val actorSystem = system
  implicit val timeout = Timeout(1.seconds)

  var mongoProps: MongodProps = _

  override def beforeAll() {
    mongoProps = mongoStart()  // port = 12345 & version = Version.2.3.0
  }

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    mongoStop(mongoProps)
  }

  private val module = new ConfigModule(ConfigTest.mockConfig) {
    override def authenticator = auth
  }

  private def createActor(name: String) =
    system.actorOf(Props(classOf[LogInActor], module), name)

  test("Successful login simple") {
    val loginActor = TestActorRef(new LogInActor(module))
    val loginResult = loginActor ? LogIn("user1", "pass1")
    val Success(LogInSuccess(returnedUser)) = loginResult.value.get
    assertThat(returnedUser).isSameAs(user)
  }

  test("Successful login") {
    createActor("success") ! LogIn("user1", "pass1")
    expectMsg(LogInSuccess(user))
  }

  test("Unsuccessful login simple") {
    val loginActor = TestActorRef(new LogInActor(module))
    val loginResult = loginActor ? LogIn("user2", "pass2")
    val Success(LogInFailure(ex)) = loginResult.value.get
    assertThat(ex).isSameAs(loginFailureException)
  }

  test("Unsuccessful login") {
    createActor("fail") ! LogIn("user2", "pass2")
    expectMsg(LogInFailure(loginFailureException))
  }

  test("Sending wrong messages to the actor") {
    val actor = createActor("WrongMessages")
    actor ! "some string"
    actor ! 123
    actor ! new Object
    actor ! "test"
    expectNoMsg(200 millis)
  }
}
