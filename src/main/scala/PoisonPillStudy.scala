import akka.actor.{Actor, ActorRef, ActorSystem, Inbox, PoisonPill,Props}
import scala.concurrent.duration._
import scala.concurrent.Await

class ParentActor extends Actor {

  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: Exception => Resume
    }

  def receive = {
    case p: Props => sender() ! context.actorOf(p)
  }
}

class ChildActor extends Actor {
  def receive = {
    case s: String => println(s)
    case e: Exception => throw e
  }
}

object PoisonPillStudy extends App {
  val system = ActorSystem("mustResume")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val supervisor = system.actorOf(Props[ParentActor], "parentActor")

  supervisor ! Props[ChildActor]
  val child = inbox.receive(5.seconds).asInstanceOf[ActorRef]

  child ! "Hello"
  println(child)
  inbox.watch(child)
  child ! new Exception("DontStop")
  println(child)
  child ! "こんにちは"
  println(child)
  child ! PoisonPill
  child ! "さようなら"
  println(child)
  Await.ready(system.terminate(), Duration.Inf)
}
