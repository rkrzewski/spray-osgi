package spray.osgi.impl

import scala.util.Failure
import scala.util.Success
import com.typesafe.config.Config
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.io.IO
import akka.io.Tcp
import akka.pattern.ask
import akka.pattern.AskTimeoutException
import akka.util.Timeout
import spray.can.Http
import spray.can.server.ServerSettings
import org.osgi.framework.BundleContext
import akka.event.BusLogging
import akka.event.LoggingAdapter

class SprayServer(config: Config, actorSystem: ActorSystem, ctx: BundleContext) {

  val serverSettings = ServerSettings.fromSubConfig(config.getConfig("spray.can.server"))
  val listenerSettings = ListenerSettings.fromSubConfig(config.getConfig("spray.can.server.listener"))

  val http = IO(Http)(actorSystem)
  val serviceActor = actorSystem.actorOf(Props(classOf[RouteManager]))
  val routeServiceTracker = new RouteServiceTracker(ctx, serviceActor)
  val staticResourcesTracker = new StaticResourcesTracker(ctx, serviceActor)(actorSystem)
  routeServiceTracker.open()
  staticResourcesTracker.open()

  http.ask(Http.Bind(
    serviceActor,
    listenerSettings.interface,
    listenerSettings.port,
    listenerSettings.backlog,
    listenerSettings.socketOptions,
    Some(serverSettings)))(Timeout(listenerSettings.bindTimeout)).onComplete {
    case Success(b: Http.Bound) =>
      println("server started")
    case Success(Tcp.CommandFailed(b: Http.Bind)) =>
      println(
        "Binding failed. Switch on DEBUG-level logging for `akka.io.TcpListener` to log the cause.")
    case Failure(e: AskTimeoutException) =>
      println("server start timeout")
    case _ =>
      println("server start failure")
  }(actorSystem.dispatcher)

  def shutdown(): Unit = {
    routeServiceTracker.close()
    staticResourcesTracker.close()
    serviceActor ! PoisonPill
    http.ask(Http.CloseAll)(Timeout(listenerSettings.bindTimeout)).onComplete {
      case Success(Http.ClosedAll) =>
        println("server stopped")
      case Failure(e: AskTimeoutException) =>
        printl("server stop timeout")
      case _ =>
        println("server stop failure", e)
    }(actorSystem.dispatcher)
  }
}