import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, ActorSystem, DeathPactException, Inbox, OneForOneStrategy, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}

case class IsPrimeMessage(numerator: Int)

case class AnswerMessage(isPrime: Boolean)

case class PrimeSearch(numList: Seq[Int])

class PrimeSearcher extends Actor {
  def isPrime(n: Int): Boolean = if (n < 2) false else !((2 until n - 1) exists (n % _ == 0))

  def receive = {
    case m@IsPrimeMessage(num) =>
      val answer = Try {
        AnswerMessage(isPrime(num))
      } match {
        case Success(a) => a
        case Failure(e) =>
          self.forward(m)
          throw e
      }
      println(s" isPrime(${num}) : ${answer}")
      sender() ! answer
  }
}

class PrimeSearchManager extends Actor {
  var primeSearchSender = Actor.noSender
  var sum = 0
  var answerCount = 0
  var total = 0

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, 10.seconds) {
      case _: ArithmeticException => {
        println("Restart by ArithmeticException")
        Restart
      }
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Stop
      case _: DeathPactException => Stop
      case _: Exception => Restart
    }

  val router = {
    val routes = Vector.fill(4) {
      ActorRefRoutee(context.actorOf(Props[PrimeSearcher]))
    }
    Router(RoundRobinRoutingLogic(), routes)
  }

  def receive = {
    case PrimeSearch(numList) => {
      primeSearchSender = sender()
      total = numList.size
      numList.foreach(n => router.route(IsPrimeMessage(n), self))
    }
    case AnswerMessage(isPrime) => {
      answerCount += 1
      if (isPrime) sum += 1
      if (answerCount == total) primeSearchSender ! sum
    }
  }
}

object PrimeNumberSearch extends App {
  val system = ActorSystem("primeSearch")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val primeSearchManager = system.actorOf(Props[PrimeSearchManager], "primeSearchManager")
  primeSearchManager ! PrimeSearch(1010000 to 1040000)
  val result = inbox.receive(600.seconds)
  println(s"Result: ${result}")

  Await.ready(system.terminate(), Duration.Inf)
}
