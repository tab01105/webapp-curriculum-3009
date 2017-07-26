import akka.actor.{Actor, Props}

class ParentActor extends Actor {
  def receive = {
    case p: Props => sender() ! context.actorOf(p)
  }
}

class ChildActor extends Actor {
  def receive = {
    case e: Exception => throw e
  }
}

object MustResume extends App {

}
