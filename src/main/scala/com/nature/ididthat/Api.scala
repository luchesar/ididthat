package com.nature.ididthat

import spray.routing._
import akka.pattern.ask
import scala.concurrent.duration._
import LogInActor._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits._
import spray.json.DefaultJsonProtocol
import spray.http.MediaTypes._
import spray.http.HttpCharsets._
import spray.httpx.SprayJsonSupport._
import spray.http.{HttpHeader, HttpCookie}
import com.nature.ididthat.LogInActor.LogIn
import scala.util.Failure
import scala.Some
import com.nature.ididthat.LogInActor.LogInFailure
import scala.util.Success
import com.nature.ididthat.LogInActor.LogInSuccess
import spray.http.StatusCodes._

object Protocol extends DefaultJsonProtocol {
  implicit val UserFormat = jsonFormat6(User)
  implicit val PermissionFormat = jsonFormat1(Permission)
  implicit val RoleFormat = jsonFormat2(Role)
  implicit val TaskFormat = jsonFormat4(Task) //TODO: need implicit conversion for Date
  implicit val TeamFormat = jsonFormat3(Team)
}

object Cookie {
  final val UserName = "UserName"
}

class ApiActor(module: ConfigModule) extends HttpServiceActor with Api {
  override def receive = runRoute(rout)

  override def configModule = module
}

trait Api extends HttpService with ActorInjector {

  import Protocol._

  val standard = decompressRequest() &
    compressResponseIfRequested() &
    respondWithMediaType(`application/json`)

  val rout =
    (get & standard & path("login") & parameters('u.?, 'p.?)) {
      (u, p) => (u, p) match {
        case (Some(uName), Some(pass)) => login(uName, pass)
        case (Some(uName), None) => login(uName, "")
        case _ => failure(new IllegalArgumentException("user or password not provided"))
      }
    }

  def login(uName: String, pass: String): Route = {
    val loginActor = logInActor(actorRefFactory)
    val ldapTimeout = Timeout(configModule.configuration.ldapTimeout)
    onComplete(loginActor.ask(LogIn(uName, pass))(ldapTimeout)) {
      case Success(LogInSuccess(user)) =>
        setCookie(HttpCookie(Cookie.UserName, content = user.id, secure = true)) {
          complete(user)
        }
      case Success(LogInFailure(error)) => failure(error)
      case Success(_) => failure(new IllegalStateException())
      case Failure(error) => failure(error)
    }
  }

  def failure(t: Throwable): Route = {
    deleteCookie(Cookie.UserName) {
      complete(Forbidden, "The supplied authentication is not authorized to access this resource")
    }
  }
}


