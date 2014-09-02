package spray.osgi.impl

import scala.annotation.meta.setter
import scala.collection.JavaConversions.asScalaSet
import java.util.Properties
import org.osgi.framework.BundleContext
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.ConfigurationPolicy
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.can.server.ServerSettings
import akka.actor.Props
import akka.io.Tcp
import scala.util.Success
import akka.pattern.AskTimeoutException
import scala.util.Failure
import akka.actor.PoisonPill
import akka.osgi.BundleDelegatingClassLoader

@Component(
  configurationPid = "io.spray.can",
  configurationPolicy = ConfigurationPolicy.REQUIRE)
class SprayServerComponent {

  @(Reference @setter)
  var actorSystem: ActorSystem = _

  var listenerSettings: ListenerSettings = _

  var http: ActorRef = _

  var serviceActor: ActorRef = _

  var routeServiceTracker: RouteServiceTracker = _

  var staticResourcesTracker: StaticResourcesTracker = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    val sprayClassloader = classOf[ServerSettings].getClassLoader
    val classloader = BundleDelegatingClassLoader(ctx, Some(sprayClassloader))
    val config = ConfigFactory.parseProperties(properties).withFallback(ConfigFactory.load(classloader))
    val serverSettings = ServerSettings.fromSubConfig(config.getConfig("spray.can.server"))
    listenerSettings = ListenerSettings.fromSubConfig(config.getConfig("spray.can.server.listener"))

    http = IO(Http)(actorSystem)
    serviceActor = actorSystem.actorOf(Props(classOf[RouteManager]))
    routeServiceTracker = new RouteServiceTracker(ctx, serviceActor)
    routeServiceTracker.open()
    staticResourcesTracker = new StaticResourcesTracker(ctx, serviceActor)(actorSystem)
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

  }

  @Deactivate
  def deactivate: Unit = {
    routeServiceTracker.close()
    staticResourcesTracker.close()
    serviceActor ! PoisonPill
    http.ask(Http.CloseAll)(Timeout(listenerSettings.bindTimeout)).onComplete {
      case Success(Http.ClosedAll) =>
        println("server stopped")
      case Failure(e: AskTimeoutException) =>
        println("server stop timeout")
      case _ =>
        println("server stop failure")
    }(actorSystem.dispatcher)
  }

  implicit def toProprties(map: java.util.Map[String, _]): Properties =
    map.keySet().foldLeft(new Properties) { (props, key) =>
      map.get(key) match {
        case strVal: String => props.put(key, strVal)
        case _ =>
      }
      props
    }
}