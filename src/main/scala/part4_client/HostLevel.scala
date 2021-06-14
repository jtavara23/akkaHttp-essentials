package part4_client

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.util.{Failure, Success, Try}
import spray.json._

object HostLevel extends App with PaymentJsonProtocol {

  implicit val system = ActorSystem("HostLevel")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool]
  // the httpRequest send to this pool, will go to any of the available connections
  // and we might receive different Http responses in different order than the original order of the requests
  val poolFlow = Http().cachedHostConnectionPool[Int]("www.google.com")

  Source(1 to 10)
    .map(i => (HttpRequest(), i))
    .via(poolFlow)
    .map { // we gonna receive pairs
      case (Success(response), value) =>
        // VERY IMPORTANT
        response
          .discardEntityBytes() // if not, i will block the connection the response wants to go through
        s"Request $value has received response: $response"
      case (Failure(ex), value) =>
        s"Request $value has failed: $ex"
    }
//    .runWith(Sink.foreach[String](println))

  /*
  Request 2 has received response: HttpResponse(200 OK,List(Date: Mon, 14 Jun 2021 16:02:08 GMT, Expires: Wed, 01 Jan 1800 00:00:00 GMT, Cache-Control: private, max-age=0, P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info.", Server: gws, X-XSS-Protection: 0, X-Frame-Options: SAMEORIGIN, Set-Cookie: 1P_JAR=2021-06-14-16; Expires=Wed, 14 Jul 2021 16:02:08 GMT; Domain=google.com; Path=/; Secure, Set-Cookie: NID=217=vryA18A4kahKhNNpCW-UBz854812VsH1hBGFZySUP5XLF0EtXs9irssi8dONwLliaJMZKYh7yBJjvI26tBB2f846DwqzQhIkQCxBQGQ2KH1qqUCNfiSr6WxPLwTxbn2qa6eet5Tm9TeGBccLA1tItKgP7okE4XsQrev2ITNHFXo; Expires=Tue, 14 Dec 2021 16:02:08 GMT; Domain=google.com; Path=/; HttpOnly, Accept-Ranges: none, Vary: Accept-Encoding),HttpEntity.Chunked(text/html; charset=ISO-8859-1),HttpProtocol(HTTP/1.1))
  Request 4 has received response: HttpResponse(200 OK,List(Date: Mon, 14 Jun 2021 16:02:08 GMT, Expires: Wed, 01 Jan 1800 00:00:00 GMT, Cache-Control: private, max-age=0, P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info.", Server: gws, X-XSS-Protection: 0, X-Frame-Options: SAMEORIGIN, Set-Cookie: 1P_JAR=2021-06-14-16; Expires=Wed, 14 Jul 2021 16:02:08 GMT; Domain=google.com; Path=/; Secure, Set-Cookie: NID=217=qrPyldI2GEV-pczak_BksNkofsniNqrgZzRy-S7snuKMVDlQoM8Fbk7oR5nssNV7DdwBkaxXyJVqD5vhoUU2Gopfz-sKOkR_sPwv_RBPlX8pt3h8Qk9sPLPEGKsagqEELbFEu34dwsFic5IE-tv_uu6YHZ9uFjR9jv6Z_8c3h7c; Expires=Tue, 14 Dec 2021 16:02:08 GMT; Domain=google.com; Path=/; HttpOnly, Accept-Ranges: none, Vary: Accept-Encoding),HttpEntity.Chunked(text/html; charset=ISO-8859-1),HttpProtocol(HTTP/1.1))
  Request 1 has received response: HttpResponse(200 OK,List(Date: Mon, 14 Jun 2021 16:02:08 GMT, Expires: Wed, 01 Jan 1800 00:00:00 GMT, Cache-Control: private, max-age=0, P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info.", Server: gws, X-XSS-Protection: 0, X-Frame-Options: SAMEORIGIN, Set-Cookie: 1P_JAR=2021-06-14-16; Expires=Wed, 14 Jul 2021 16:02:08 GMT; Domain=google.com; Path=/; Secure, Set-Cookie: NID=217=mzpOqmdOFLkQi8d0L_VNqWj7KeCQXcdbn3v6qsZpe9PwEOd9hUhTIE0DuIAINZGF3d3y4rmNRlZch4Fjqyf3mwUQNBcZvi6dnqE9BFQzEEu0j7GwMisJf4OJRAKptfr8gZXUaWMThoroqx5U1dsXFhhJMKXZVfeTExKSnoD9SL0; Expires=Tue, 14 Dec 2021 16:02:08 GMT; Domain=google.com; Path=/; HttpOnly, Accept-Ranges: none, Vary: Accept-Encoding),HttpEntity.Chunked(text/html; charset=ISO-8859-1),HttpProtocol(HTTP/1.1))
  Request 3 has received response: HttpResponse(200 OK,List(Date: Mon, 14 Jun 2021 16:02:08 GMT, Expires: Wed, 01 Jan 1800 00:00:00 GMT, Cache-Control: private, max-age=0, P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info.", Server: gws, X-XSS-Protection: 0, X-Frame-Options: SAMEORIGIN, Set-Cookie: 1P_JAR=2021-06-14-16; Expires=Wed, 14 Jul 2021 16:02:08 GMT; Domain=google.com; Path=/; Secure, Set-Cookie: NID=217=Fl38eO1_ulS4zv0U-dkYcqIOmfhyQcLw2LKaoreEmjuHSwVFIBIhAOGyBiMMQMDbpflw2FxGZYw7oiG-TLYzobfKTWC9JP25T9x3k4Pgdk7LdhArMuQfX0mlA8YzrOy3668Wyt-xBh1OOLmiiW0l_7QOPKqwGn5svsId6t5f9vE; Expires=Tue, 14 Dec 2021 16:02:08 GMT; Domain=google.com; Path=/; HttpOnly, Accept-Ranges: none, Vary: Accept-Encoding),HttpEntity.Chunked(text/html; charset=ISO-8859-1),HttpProtocol(HTTP/1.1))
  Request 7 has received response: HttpResponse(200 OK,List(Date: Mon, 14 Jun 2021 16:02:08 GMT, Expires: Wed, 01 Jan 1800 00:00:00 GMT, Cache-Control: private, max-age=0, P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info.", Server: gws, X-XSS-Protection: 0, X-Frame-Options: SAMEORIGIN, Set-Cookie: 1P_JAR=2021-06-14-16; Expires=Wed, 14 Jul 2021 16:02:08 GMT; Domain=google.com; Path=/; Secure, Set-Cookie: NID=217=UUqRpGxuXv1IF7XS7OV8aJe5vSOvtQNANhsfIp2msAsVQIFolHvQCt_IxLwAygb3j5USIxBDklHf8eLaR3G6HbPE7q1Mi26X6fK7BFhecHuvzat4DMkVLedPqPKV7fRMa0FQpk2mj_KKtHXZgR0y3p8tFD9Th0G30s4yjstr-fU; Expires=Tue, 14 Dec 2021 16:02:08 GMT; Domain=google.com; Path=/; HttpOnly, Accept-Ranges: none, Vary: Accept-Encoding),HttpEntity.Chunked(text/html; charset=ISO-8859-1),HttpProtocol(HTTP/1.1))
  Request 5 has received response: HttpResponse(200 OK,List(Date: Mon, 14 Jun 2021 16:02:08 GMT, Expires: Wed, 01 Jan 1800 00:00:00 GMT, Cache-Control: private, max-age=0, P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info.", Server: gws, X-XSS-Protection: 0, X-Frame-Options: SAMEORIGIN, Set-Cookie: 1P_JAR=2021-06-14-16; Expires=Wed, 14 Jul 2021 16:02:08 GMT; Domain=google.com; Path=/; Secure, Set-Cookie: NID=217=a-138NiJLvGMuxkSMyr2fG69-Nnd-YafsalkvpeAZONpkmV0z2YOWl06q5GQVsg9RzEmtwIswznxmisIEE2P-HbToAhTE1aCkmgGHwdo1GY0Fldx2kLvHWr6IfWvZW4MtNFY71OsqmByx9OCQCHDp-IOU7uI63_OwJsI2heFNws; Expires=Tue, 14 Dec 2021 16:02:08 GMT; Domain=google.com; Path=/; HttpOnly, Accept-Ranges: none, Vary: Accept-Encoding),HttpEntity.Chunked(text/html; charset=ISO-8859-1),HttpProtocol(HTTP/1.1))
  Request 8 has received response: HttpResponse(200 OK,List(Date: Mon, 14 Jun 2021 16:02:08 GMT, Expires: Wed, 01 Jan 1800 00:00:00 GMT, Cache-Control: private, max-age=0, P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info.", Server: gws, X-XSS-Protection: 0, X-Frame-Options: SAMEORIGIN, Set-Cookie: 1P_JAR=2021-06-14-16; Expires=Wed, 14 Jul 2021 16:02:08 GMT; Domain=google.com; Path=/; Secure, Set-Cookie: NID=217=rhCmX7TyA2AD2zFJnIhSeomUmIWPE6pVc94NbaldQmcWDg-yt6-0LrpqjURcXBPQrYH4kYDzMPz1FoupCR_qW1rbEJAYhCgtNck4DKlmJ6MRIGXsWONgHF8EKwpvgAF5kTyyGpzJCEDAH3eKF758j6-xWrhPEwUuqjEq4ac8p0I; Expires=Tue, 14 Dec 2021 16:02:08 GMT; Domain=google.com; Path=/; HttpOnly, Accept-Ranges: none, Vary: Accept-Encoding),HttpEntity.Chunked(text/html; charset=ISO-8859-1),HttpProtocol(HTTP/1.1))
  Request 6 has received response: HttpResponse(200 OK,List(Date: Mon, 14 Jun 2021 16:02:08 GMT, Expires: Wed, 01 Jan 1800 00:00:00 GMT, Cache-Control: private, max-age=0, P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info.", Server: gws, X-XSS-Protection: 0, X-Frame-Options: SAMEORIGIN, Set-Cookie: 1P_JAR=2021-06-14-16; Expires=Wed, 14 Jul 2021 16:02:08 GMT; Domain=google.com; Path=/; Secure, Set-Cookie: NID=217=45w1tHtxDgTg7V9mKN0RSI2FnENqn881plc9DKJ-1-jsb6rv-GGQDpuE96HPu-hQYqIxnijounywNYhFzKcCl2XQbeshM9ous0CIq-_C3QmsvxJb7JCBioxXSv5OConPTroASpMdhx2clf_2R3d1Uhivoe-nwjRJFRemk_B8F-Y; Expires=Tue, 14 Dec 2021 16:02:08 GMT; Domain=google.com; Path=/; HttpOnly, Accept-Ranges: none, Vary: Accept-Encoding),HttpEntity.Chunked(text/html; charset=ISO-8859-1),HttpProtocol(HTTP/1.1))
  Request 9 has received response: HttpResponse(200 OK,List(Date: Mon, 14 Jun 2021 16:02:09 GMT, Expires: Wed, 01 Jan 1800 00:00:00 GMT, Cache-Control: private, max-age=0, P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info.", Server: gws, X-XSS-Protection: 0, X-Frame-Options: SAMEORIGIN, Set-Cookie: 1P_JAR=2021-06-14-16; Expires=Wed, 14 Jul 2021 16:02:09 GMT; Domain=google.com; Path=/; Secure, Set-Cookie: NID=217=uFQOzYE3zWh9gsymCQFdYAsZGuC-IclUUtA7zJUy8OzIUQe5M38nMJjX3ePLLbQcvFd8qgfryC5JRALiG2rnwdZmgtuc7IE7j1MdlsPghgo4VXnuY9vIza6kXfMy8Bjmmlt7sOjCSUX6F3zuPdT514SzSKG0Lkfxf__-s1Zszuc; Expires=Tue, 14 Dec 2021 16:02:09 GMT; Domain=google.com; Path=/; HttpOnly, Accept-Ranges: none, Vary: Accept-Encoding),HttpEntity.Chunked(text/html; charset=ISO-8859-1),HttpProtocol(HTTP/1.1))
  Request 10 has received response: HttpResponse(200 OK,List(Date: Mon, 14 Jun 2021 16:02:09 GMT, Expires: Wed, 01 Jan 1800 00:00:00 GMT, Cache-Control: private, max-age=0, P3P: CP="This is not a P3P policy! See g.co/p3phelp for more info.", Server: gws, X-XSS-Protection: 0, X-Frame-Options: SAMEORIGIN, Set-Cookie: 1P_JAR=2021-06-14-16; Expires=Wed, 14 Jul 2021 16:02:09 GMT; Domain=google.com; Path=/; Secure, Set-Cookie: NID=217=xYJpg8M-ntFXAaUHN-3yrzS2zOeqp1huC780B6oCPxltZ38lsQocgr137EnPn687NRP3htvZWMZ6VeE1HIHnfrueNsR7ZyS_QwmzPIU0nO386ThUiQrwYrMOmvO8w0U-oerWjPYnQOjKjKcU_WbjqqB59-9MG_88qUHETZJ4rWs; Expires=Tue, 14 Dec 2021 16:02:09 GMT; Domain=google.com; Path=/; HttpOnly, Accept-Ranges: none, Vary: Accept-Encoding),HttpEntity.Chunked(text/html; charset=ISO-8859-1),HttpProtocol(HTTP/1.1))
   * */

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
      (
        HttpRequest(
          HttpMethods.POST,
          uri = Uri("/api/payments"),
          entity = HttpEntity(
            ContentTypes.`application/json`,
            paymentRequest.toJson.prettyPrint
          )
        ),
        UUID.randomUUID().toString // now we sent an UUID as part of the request
    )
  )
  Source(serverHttpRequests)
    .via(Http().cachedHostConnectionPool[String]("localhost", 8080))
    .runForeach { // (Try[HttpResponse], String)
      case (
          Success(response @ HttpResponse(StatusCodes.Forbidden, _, _, _)),
          orderId
          ) =>
        println(s"The order ID $orderId was not allowed to proceed: $response")
      case (Success(response), orderId) =>
        println(
          s"The order ID $orderId was successful and returned the response: $response"
        )
      // do something with the order ID: dispatch it, send a notification to the customer, etc
      case (Failure(ex), orderId) =>
        println(s"The order ID $orderId could not be completed: $ex")
    }

  /*
   The order ID 01625662-7685-4b27-a31f-01ecd81a64fd was successful and returned the response: HttpResponse(200 OK,List(Server: akka-http/10.1.7, Date: Mon, 14 Jun 2021 16:07:15 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,OK),HttpProtocol(HTTP/1.1))
  The order ID be33511c-8b92-46f5-a2f0-e41212a81b71 was successful and returned the response: HttpResponse(200 OK,List(Server: akka-http/10.1.7, Date: Mon, 14 Jun 2021 16:07:15 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,OK),HttpProtocol(HTTP/1.1))
  The order ID 3ba77e82-18f2-46a1-a4d7-80dd12c8a2df was not allowed to proceed: HttpResponse(403 Forbidden,List(Server: akka-http/10.1.7, Date: Mon, 14 Jun 2021 16:07:15 GMT),HttpEntity.Strict(text/plain; charset=UTF-8,The request was a legal request, but the server is refusing to respond to it.),HttpProtocol(HTTP/1.1))
   */
  // beneficial when we have: high-volume, low-latency requests
}
