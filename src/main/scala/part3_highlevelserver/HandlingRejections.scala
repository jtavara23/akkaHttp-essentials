package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{
  MethodRejection,
  MissingQueryParamRejection,
  Rejection,
  RejectionHandler
}

object HandlingRejections extends App {

  implicit val system = ActorSystem("HandlingRejections")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // Rejection handlers
  val badRequestHandler: RejectionHandler = { rejections: Seq[Rejection] =>
    println(s"I have encountered rejections: $rejections")
    Some(complete(StatusCodes.BadRequest))
  }

  val forbiddenHandler: RejectionHandler = { rejections: Seq[Rejection] =>
    println(s"I have encountered rejections: $rejections")
    Some(complete(StatusCodes.Forbidden))
  }

  val simpleRouteWithHandlers =
    handleRejections(badRequestHandler) { // handle rejections from the top level
      // define server logic inside
      path("api" / "myEndpoint") {
        get {
          complete(StatusCodes.OK)
          //http://localhost:8080/api/myEndpoint?myParam=2 -> GET - OK
        } ~
          post {
            handleRejections(forbiddenHandler) { // handle rejections WITHIN
              parameter('myParam) { _ =>
                complete(StatusCodes.OK)
              //http://localhost:8080/api/myEndpoint?myParam=2 -> POST - OK
              }
            }
            //  POST http://localhost:8080/api/myEndpoint?superParam=2 -> I have encountered rejections: List(MissingQueryParamRejection(myParam))
          }
        // PUT http://localhost:8080/api/myEndpoint?myParam=2 = -> I have encountered rejections: List(MethodRejection(HttpMethod(GET)), MethodRejection(HttpMethod(POST)))
      }
      // GET/POST http://localhost:8080/api/mySupEnd -> I have encountered rejections: List()

    }

//  Http().bindAndHandle(simpleRouteWithHandlers, "localhost", 8080)

  /*
   * Another way to handle REJECTIONS */

  val simpleRoute =
    path("api" / "myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } ~
        parameter('id) { _ =>
          complete(StatusCodes.OK)
        }
    }

  // list(method rejection, query param rejection)
  implicit val customRejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case m: MissingQueryParamRejection =>
        println(s"I got a query param rejection: $m")
        complete("Rejected query param!") // return a ROUTE
    }
    .handle {
      case m: MethodRejection =>
        println(s"I got a method rejection: $m")
        complete("Rejected method!") // return a ROUTE
    }
    .result()

  // having an implicit rejctionHnadler we can be sealing a route

  Http().bindAndHandle(simpleRoute, "localhost", 8080)
//http://localhost:8080/api/myEndpoint?iod=2 -> POST -> I got a query param rejection: MissingQueryParamRejection(id)
//http://localhost:8080/api/myEndpoint?iod=2 -> GET -> OK

}
