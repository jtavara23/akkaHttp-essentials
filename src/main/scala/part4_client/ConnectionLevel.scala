package part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.util.{Failure, Success}

import spray.json._

object ConnectionLevel extends App with PaymentJsonProtocol {

  implicit val system = ActorSystem("ConnectionLevel")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  //open a connection
  val connectionFlow = Http().outgoingConnection("www.google.com")
  // to send request to a single connection
  // every time, a new connection gets created
  def oneOffRequest(request: HttpRequest) =
    Source.single(request).via(connectionFlow).runWith(Sink.head)

  oneOffRequest(HttpRequest()).onComplete {
    case Success(response) => println(s"Got successful response: $response")
    case Failure(ex)       => println(s"Sending the request failed: $ex")
  }

  /*
    A small payments system
   */

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
        uri = Uri("/api/payments"),
        entity = HttpEntity(
          ContentTypes.`application/json`,
          paymentRequest.toJson.prettyPrint
        )
    )
  )

  Source(serverHttpRequests)
    .via(Http().outgoingConnection("localhost", 8080))
    .to(Sink.foreach[HttpResponse](println))
    .run()
  /*
  HttpResponse(200 OK,List(Server: akka-http/10.1.7, Date: Mon, 14 Jun 2021 15:49:41 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,OK),HttpProtocol(HTTP/1.1))
  HttpResponse(403 Forbidden,List(Server: akka-http/10.1.7, Date: Mon, 14 Jun 2021 15:49:42 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,The request was a legal request, but the server is refusing to respond to it.),HttpProtocol(HTTP/1.1))
  HttpResponse(200 OK,List(Server: akka-http/10.1.7, Date: Mon, 14 Jun 2021 15:49:42 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,OK),HttpProtocol(HTTP/1.1))
   */

  //another way
//  Source(serverHttpRequests)
//    .via(Http().outgoingConnection("localhost", 8080))
//    .runWith(Sink.foreach[HttpResponse](println))

}
