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
import spray.osgi.RouteManager
import akka.actor.ActorRef

class SprayServer(config: Config, actorSystem: ActorSystem, ctx: BundleContext) extends RouteManager {

  val serverSettings = ServerSettings.fromSubConfig(config.getConfig("spray.can.server"))
  val listenerSettings = ListenerSettings.fromSubConfig(config.getConfig("spray.can.server.listener"))

  val http = IO(Http)(actorSystem)
  val serviceActor = actorSystem.actorOf(Props(classOf[RouteManagerActor]))
  val routeServiceTracker = new RouteProvidersTracker(ctx, serviceActor)
  routeServiceTracker.open()

  val log: LoggingAdapter = new BusLogging(actorSystem.eventStream, "Spray server", this.getClass)

  http.ask(Http.Bind(
    serviceActor,
    listenerSettings.interface,
    listenerSettings.port,
    listenerSettings.backlog,
    listenerSettings.socketOptions,
    Some(serverSettings)))(Timeout(listenerSettings.bindTimeout)).onComplete {
    case Success(b: Http.Bound) =>
      log.info("server started")
    case Success(Tcp.CommandFailed(b: Http.Bind)) =>
      log.error(
        "Binding failed. Switch on DEBUG-level logging for `akka.io.TcpListener` to log the cause.")
    case Success(u) =>
      log.error(s"server start failure, unexected message $u")
    case Failure(e: AskTimeoutException) =>
      log.error("server start timeout")
    case Failure(e) =>
      log.error("server start failure", e)
  }(actorSystem.dispatcher)
  
  def ref: ActorRef = 
    serviceActor

  def shutdown(): Unit = {
    routeServiceTracker.close()
    serviceActor ! PoisonPill
    http.ask(Http.CloseAll)(Timeout(listenerSettings.bindTimeout)).onComplete {
      case Success(Http.ClosedAll) =>
        log.info("server stopped")
      case Success(u) =>
        log.error(s"server stop failure, unexected message $u")
      case Failure(e: AskTimeoutException) =>
        log.error("server stop timeout")
      case Failure(e) =>
        log.error("server stop failure", e)
    }(actorSystem.dispatcher)
  }
}