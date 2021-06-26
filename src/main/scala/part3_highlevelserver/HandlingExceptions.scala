package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler

object HandlingExceptions extends App {

  implicit val system = ActorSystem("HandlingExceptions")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val simpleRoute =
    path("api" / "people") {
      get {
        // directive that throws some exception
        throw new RuntimeException("Getting all the people took too long")
      } ~
        post {
          parameter('id) { id =>
            if (id.length > 2)
              throw new NoSuchElementException(
                s"Parameter $id cannot be found in the database, TABLE FLIP!"
              )

            complete(StatusCodes.OK)
          }
        }
    }

//  / the default will handle with eror 500
  implicit val customExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: RuntimeException =>
      //we cam do some implementation here to handle the exception
      complete(StatusCodes.NotFound, e.getMessage)
    case e: IllegalArgumentException =>
      complete(StatusCodes.BadRequest, e.getMessage)
  }

//  Http().bindAndHandle(simpleRoute, "localhost", 8080)

//http://localhost:8080/api/people - > error 404 -> Getting all the people took too long
//http://localhost:8080/api/people?id=23342 - > error 400  -> Parameter 23342 cannot be found in the database, TABLE FLIP!

  /*
  ============ Separate way =======================
   */
  val runtimeExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: RuntimeException =>
      complete(StatusCodes.NotFound, e.getMessage)
  }

  val noSuchElementExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: NoSuchElementException =>
      complete(StatusCodes.BadRequest, e.getMessage)
  }

  val delicateHandleRoute =
    handleExceptions(runtimeExceptionHandler) {
      path("api" / "people") {
        get {
          // directive that throws some exception
          throw new RuntimeException("Getting all the people took too long")
        } ~
          handleExceptions(noSuchElementExceptionHandler) {
            post {
              parameter('id) { id =>
                if (id.length > 2)
                  throw new NoSuchElementException(
                    s"Parameter $id cannot be found in the database, TABLE FLIP!"
                  )

                complete(StatusCodes.OK)
              }
            }
          }
      }
    }

  Http().bindAndHandle(delicateHandleRoute, "localhost", 8080)

}
