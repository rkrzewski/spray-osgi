package test.osgi.actor.consumer

import org.osgi.service.component.annotations.Component
import test.osgi.actor.api.MyActor
import scala.annotation.meta.setter
import org.osgi.service.component.annotations.Reference
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.Success
import scala.util.Failure
import akka.pattern.AskTimeoutException
import org.osgi.service.component.annotations.Activate

@Component
class ConsumerCmpn {

  @(Reference @setter)
  var actorSystem: ActorSystem = _

  @(Reference @setter)
  var myActor: MyActor = _

  @Activate
  def activate: Unit = {
    implicit val executionContext = actorSystem.dispatcher
    implicit val timeout = Timeout(1.second)
    import MyActor._
    (myActor ? Ping).onComplete {
      case Success(Pong) =>
        println("got Pong")
      case Success(_) =>
        println("huh?")
      case Failure(_: AskTimeoutException) =>
        println("timeout")
      case Failure(e) =>
        e.printStackTrace()
    }
  }
}