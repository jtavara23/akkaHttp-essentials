package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import part2_lowlevelserver.HttpsContext

object HighLevelIntro extends App {

  implicit val system = ActorSystem("HighLevelIntro")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // directives
  import akka.http.scaladsl.server.Directives._

  val simpleRoute: Route =
    path("ho") { // DIRECTIVE -  filter http requests
      complete(StatusCodes.OK) // DIRECTIVE -  specify how it responds
    } // equivalent directives for get, put, patch, delete, head, options
  // as it is not specified

  val pathGetRoute: Route =
    path("home") {
      get {
        complete(StatusCodes.OK)
      }
    }

  // chaining directives with ~ (meaning: otherwise)
  // without it, it only process whatever is after it
  val chainedRoute: Route =
    path("myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } /* VERY IMPORTANT ---> */ ~
        post {
          complete(StatusCodes.Forbidden)
        }
    } ~
      path("home") {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            """
            |<html>
            | <body>
            |   Hello from the high level Akka HTTP!
            | </body>
            |</html>
          """.stripMargin
          )
        )
      } // Routing tree

  Http().bindAndHandle(chainedRoute, "localhost", 8080)

}
