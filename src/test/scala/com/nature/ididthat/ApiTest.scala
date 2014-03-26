package com.nature.ididthat

import Protocol._
import spray.testkit.ScalatestRouteTest
import org.mockito.Mockito._
import scala.util.{Failure, Success}
import org.fest.assertions.Assertions._
import org.scalatest.FunSuite
import spray.http.MediaTypes._
import spray.http.HttpCharsets._
import spray.httpx.SprayJsonSupport._
import spray.http._
import spray.http.HttpHeaders.{`Set-Cookie`, `Accept-Encoding`}
import spray.http.HttpEncodings._
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import StatusCodes._
import scala.Some

class ApiTest
  extends FunSuite
  with ScalatestRouteTest
  with Api
  with ActorInjector {

  def actorRefFactory = system

  val auth = mock(classOf[LdapAuthenticator])
  val user1 = User("user1", "John Smith", "John", "Smith", "john@smith.com", Set("admin", "user"))
  val loginFailureException = new Exception()
  val authenticateException = new RuntimeException()
  when(auth.authenticate("user1", "pass1")).thenReturn(Success(user1))
  when(auth.authenticate("user1", "")).thenReturn(Success(user1))
  when(auth.authenticate("user2", "pass2")).thenReturn(Failure(loginFailureException))

  override def configModule = new ConfigModule(ConfigTest.mockConfig) {
    override def authenticator = auth
  }

  test("Successful login") {
    Get("/login?u=user1&p=pass1") ~> rout ~> check {
      status === OK
      assertThat(response.encoding).isEqualTo(HttpEncodings.identity)
      assertThat(mediaType).isEqualTo(`application/json`)
      assertThat(charset).isEqualTo(`UTF-8`)
<<<<<<< HEAD
      assertThat(responseAs[User]).isEqualTo(user)
      header[`Set-Cookie`] === Some(`Set-Cookie`(
        HttpCookie(Api.CookieName,
          content = "user1",
          secure = true)
//          expires = Some(e))
      ))
=======
      assertThat(responseAs[User]).isEqualTo(user1)

      val Some(`Set-Cookie`(c)) = header[`Set-Cookie`]
      assertThat(c.name).isEqualTo(Cookie.UserName)
      assertThat(c.secure).isEqualTo(true)
      assertThat(c.content).isEqualTo("user1")
>>>>>>> 4af4e62beba512d08d29babbda0ecd173694c6ab
    }
  }

  test("Successful login compressed") {
    Get("/login?u=user1&p=pass1") ~> `Accept-Encoding`(gzip, deflate) ~> rout ~> check {
      status === OK
      assertThat(response.encoding).isEqualTo(HttpEncodings.gzip)
    }
    Get("/login?u=user1&p=pass1") ~> `Accept-Encoding`(deflate) ~> rout ~> check {
      status === OK
      assertThat(response.encoding).isEqualTo(HttpEncodings.deflate)
    }
  }

  test("Successful login with empty password") {
    Get("/login?u=user1&p=") ~> rout ~> check {
      status === OK
      assertThat(response.encoding).isEqualTo(HttpEncodings.identity)
      assertThat(mediaType).isEqualTo(`application/json`)
      assertThat(charset).isEqualTo(`UTF-8`)
      assertThat(responseAs[User]).isEqualTo(user1)
    }
  }

  test("Login missing or wrong parameters") {
    when(auth.authenticate("user", "")).thenReturn(Failure(loginFailureException))
    Get("/login?u=user") ~> rout ~> check {
      assertForbidden()
    }

    when(auth.authenticate(null, "pass")).thenReturn(Failure(loginFailureException))
    Get("/login?p=pass") ~> rout ~> check {
      assertForbidden()
    }
    when(auth.authenticate(null, null)).thenReturn(Failure(loginFailureException))
    Get("/login") ~> rout ~> check {
      assertForbidden()
    }
    when(auth.authenticate("user", "pass")).thenReturn(Failure(loginFailureException))
    Get("/login?u=user&p=pass&other=someother") ~> rout ~> check {
      assertForbidden()
    }
  }

  test("Failure login") {
    Get("/login?u=user2&p=pass2") ~> rout ~> check {
      assertForbidden()
    }
  }

  test("Successful login and then unsuccesful login") {
    Get("/login?u=user1&p=pass1") ~> rout ~> check {
      val Some(`Set-Cookie`(c)) = header[`Set-Cookie`]
      assertThat(c.content).isEqualTo("user1")
    }

    when(auth.authenticate("user", "pas")).thenReturn(Failure(loginFailureException))
    Get("/login?u=user1&p=pas") ~> rout ~> check {
      assertForbidden()
    }
  }

  private def assertForbidden() {
    assertThat(status).isEqualTo(Forbidden)
    assertThat(responseAs[String]).isEqualTo("The supplied authentication is not authorized to access this resource")

    val Some(`Set-Cookie`(c)) = header[`Set-Cookie`]
    assertThat(c.name).isEqualTo(Cookie.UserName)
    assertThat(c.content).isEqualTo("deleted")
    assertThat(c.expires).isEqualTo(Some(DateTime.MinValue))
  }
}
