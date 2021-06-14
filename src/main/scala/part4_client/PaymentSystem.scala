package part4_client

import akka.pattern.ask

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import spray.json._

//model
case class CreditCard(serialNumber: String,
                      securityCode: String,
                      account: String)

//domain ADT
object PaymentSystemDomain {
  case class PaymentRequest(creditCard: CreditCard,
                            receiverAccount: String,
                            amount: Double)
  case object PaymentAccepted
  case object PaymentRejected
}

trait PaymentJsonProtocol extends DefaultJsonProtocol {
  implicit val creditCardFormat = jsonFormat3(CreditCard)
  implicit val paymentRequestFormat = jsonFormat3(
    PaymentSystemDomain.PaymentRequest
  )
}

class PaymentValidator extends Actor with ActorLogging {
  import PaymentSystemDomain._

  override def receive: Receive = {
    case PaymentRequest(
        CreditCard(serialNumber, _, senderAccount),
        receiverAccount,
        amount
        ) =>
      log.info(
        s"$senderAccount is trying to send $amount dollars to $receiverAccount"
      )
      if (serialNumber == "1234-1234-1234-1234") sender() ! PaymentRejected
      else sender() ! PaymentAccepted
  }
}

object PaymentSystem
    extends App
    with PaymentJsonProtocol
    with SprayJsonSupport {

  // microservice for payments
  implicit val system = ActorSystem("PaymentSystem")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import PaymentSystemDomain._

  val paymentValidator =
    system.actorOf(Props[PaymentValidator], "paymentValidator")
  implicit val timeout = Timeout(2 seconds)

  val paymentRoute =
    path("api" / "payments") {
      post {
        entity(as[PaymentRequest]) { paymentRequest =>
          val validationResponseFuture =
            (paymentValidator ? paymentRequest).map {
              case PaymentRejected => StatusCodes.Forbidden
              case PaymentAccepted => StatusCodes.OK
              case _               => StatusCodes.BadRequest
            }

          complete(validationResponseFuture)
        }
      }
    }

  Http().bindAndHandle(paymentRoute, "localhost", 8080)

  //receive
  /*
  [INFO] [06/14/2021 10:53:04.884] [akka://PaymentSystem/user/paymentValidator] tx-test-account is trying to send 99.0 dollars to rtjvm-store-account
  [INFO] [06/14/2021 10:53:04.966] [akka://PaymentSystem/user/paymentValidator] tx-daniels-account is trying to send 99.0 dollars to rtjvm-store-account
  [INFO] [06/14/2021 10:53:04.974] [akka://PaymentSystem/user/paymentValidator] my-awesome-account is trying to send 99.0 dollars to rtjvm-store-account*/
}
