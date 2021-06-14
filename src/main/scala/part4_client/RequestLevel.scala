package part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.util.{Failure, Success}
import spray.json._

object RequestLevel extends App with PaymentJsonProtocol {

  implicit val system = ActorSystem("RequestLevelAPI")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val responseFuture =
    Http().singleRequest(HttpRequest(uri = "http://www.google.com"))

  responseFuture.onComplete {
    case Success(response) =>
      // VERY IMPORTANT
      response.discardEntityBytes()
      println(s"The request was successful and returned: $response")
    case Failure(ex) =>
      println(s"The request failed with: $ex")
  }

  import PaymentSystemDomain._

  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniels-account"),
    CreditCard("1234-1234-4321-4321", "321", "my-awesome-account")
  )

  val paymentRequests = creditCards.map(
    creditCard => PaymentRequest(creditCard, "rtjvm-store-account", 99)
  )
  val serverHttpRequests = paymentRequests.map(
    paymentRequest =>
      HttpRequest(
        HttpMethods.POST,
        uri = "http://localhost:8080/api/payments",
        entity = HttpEntity(
          ContentTypes.`application/json`,
          paymentRequest.toJson.prettyPrint
        )
    )
  )

  Source(serverHttpRequests)
    .mapAsyncUnordered(10)(request => Http().singleRequest(request)) //we can use: mapAsync, as well
    .runForeach(println)

  /*
HttpResponse(403 Forbidden,List(Server: akka-http/10.1.7, Date: Mon, 14 Jun 2021 16:22:10 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,The request was a legal request, but the server is refusing to respond to it.),HttpProtocol(HTTP/1.1))
HttpResponse(200 OK,List(Server: akka-http/10.1.7, Date: Mon, 14 Jun 2021 16:22:10 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,OK),HttpProtocol(HTTP/1.1))
HttpResponse(200 OK,List(Server: akka-http/10.1.7, Date: Mon, 14 Jun 2021 16:22:10 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,OK),HttpProtocol(HTTP/1.1))
 */

}
