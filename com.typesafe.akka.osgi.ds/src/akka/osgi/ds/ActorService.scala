package akka.osgi.ds

import akka.actor.ActorRef
import akka.actor.ScalaActorRef

trait ActorService {
  def apply(): ActorRef
}
object ActorService {
  implicit def scalaActorRef(service: ActorService): ScalaActorRef =
    service()
  implicit def ask(service: ActorService) = 
    akka.pattern.ask(service())
}