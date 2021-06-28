package part3_highlevelserver

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson} //special library
import spray.json._

import scala.util.{Failure, Success}

object SecurityDomain extends DefaultJsonProtocol {
  case class LoginRequest(username: String, password: String)
  implicit val loginRequestFormat = jsonFormat2(LoginRequest)
}

object JwtAuthorization extends App with SprayJsonSupport {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer() // for  Http().bindAndHandle
  import system.dispatcher
  import SecurityDomain._

  val superSecretPasswordDb =
    Map("admin" -> "admin", "daniel" -> "Rockthejvm1!")

  def checkPassword(username: String, password: String): Boolean =
    superSecretPasswordDb.contains(username) && superSecretPasswordDb(username) == password

  val algorithm = JwtAlgorithm.HS256
  val secretKey = "rockthejvmsecret"

  def createToken(username: String, expirationPeriodInDays: Int): String = {
    val claims = JwtClaim(
      expiration = Some(
        System.currentTimeMillis() / 1000
          + TimeUnit.DAYS.toSeconds(expirationPeriodInDays)
      ),
      issuedAt = Some(System.currentTimeMillis() / 1000),
      issuer = Some("rockthejvm.com")
    )

    JwtSprayJson.encode(claims, secretKey, algorithm) // JWT string
  }

  def isTokenExpired(token: String): Boolean =
    JwtSprayJson.decode(token, secretKey, Seq(algorithm)) match {
      case Success(claims) =>
        claims.expiration.getOrElse(0L) < System.currentTimeMillis() / 1000
      case Failure(_) => true
    }

  def isTokenValid(token: String): Boolean =
    JwtSprayJson.isValid(token, secretKey, Seq(algorithm))

  /* ================= ROUTES =================*/
  val loginRoute =
    post {
      entity(as[LoginRequest]) {
        case LoginRequest(username, password)
            if checkPassword(username, password) =>
          val token = createToken(username, 1)
          respondWithHeader(RawHeader("Access-Token", token)) {
            complete(StatusCodes.OK)
          }
        case _ => complete(StatusCodes.Unauthorized)
      }
    }

  val authenticatedRoute =
    (path("secureEndpoint") & get) { // for every 'get'
      optionalHeaderValueByName("Authorization") {
        case Some(token) =>
          if (isTokenValid(token)) {
            if (isTokenExpired(token)) {
              complete(
                HttpResponse(
                  status = StatusCodes.Unauthorized,
                  entity = "Token expired."
                )
              )
            } else {
              complete("User accessed authorized endpoint!")
            }
          } else {
            complete(
              HttpResponse(
                status = StatusCodes.Unauthorized,
                entity = "Token is invalid, or has been tampered with."
              )
            )
          }
        case _ =>
          complete(
            HttpResponse(
              status = StatusCodes.Unauthorized,
              entity = "No token provided!"
            )
          )
      }
    }

  val route = loginRoute ~ authenticatedRoute

  Http().bindAndHandle(route, "localhost", 8080)

  /*
   *
  >>  http POST localhost:8080 < login.json

   * Response
   * HTTP/1.1 200 OK
    Access-Token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJyb2NrdGhlanZtLmNvbSIsImV4cCI6MTYyNDkyOTg5MiwiaWF0IjoxNjI0ODQzNDkyfQ.uQRN7q-1hw4RiWMgfYtoFicX_fCTHAC3Hwem2JdXL3M
    Content-Length: 2
    Content-Type: text/plain; charset=UTF-8
    Date: Mon, 28 Jun 2021 01:24:53 GMT
    Server: akka-http/10.1.7
    OK

   *
   * */
  /*
 *
  >> http GET localhost:8080/secureEndpoint "Authorization: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJyb2NrdGhlanZtLmNvbSIsImV4cCI6MTYyNDkyOTg5MiwiaWF0IjoxNjI0ODQzNDkyfQ.uQRN7q-1hw4RiWMgfYtoFicX_fCTHAC3Hwem2JdXL3M"

 * Response
  HTTP/1.1 200 OK
  Content-Length: 34
  Content-Type: text/plain; charset=UTF-8
  Date: Mon, 28 Jun 2021 01:26:06 GMT
  Server: akka-http/10.1.7
  User accessed authorized endpoint!
 */
}
