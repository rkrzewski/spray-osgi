package test.osgi.actor.impl

import org.osgi.service.component.annotations.Component
import akka.actor.ActorSystem
import scala.annotation.meta.setter
import org.osgi.service.component.annotations.Reference
import akka.actor.ActorRef
import akka.actor.Props
import test.osgi.actor.api.MyActor
import org.osgi.service.component.annotations.Activate

@Component
class MyActorComp extends MyActor {

  @(Reference @setter)
  var actorSystem: ActorSystem = _

  var myActorRef: ActorRef = _

  @Activate
  def activate: Unit = {
    myActorRef = actorSystem.actorOf(Props(classOf[MyActorImpl]))
  }

  def apply(): ActorRef =
    myActorRef
}