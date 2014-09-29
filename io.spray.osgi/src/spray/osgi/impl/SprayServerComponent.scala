package spray.osgi.impl

import java.util.Properties

import scala.annotation.meta.setter
import scala.collection.JavaConversions.asScalaSet
import scala.util.Failure
import scala.util.Success

import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.ConfigurationPolicy
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference

import com.typesafe.config.ConfigFactory

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.io.IO
import akka.io.Tcp
import akka.osgi.BundleDelegatingClassLoader
import akka.pattern.AskTimeoutException
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.can.server.ServerSettings
import spray.osgi.RouteManager

@Component(
  configurationPid = "io.spray.can",
  configurationPolicy = ConfigurationPolicy.REQUIRE)
class SprayServerComponent {

  @(Reference @setter)
  var actorSystem: ActorSystem = _

  var sprayServer: SprayServer = _

  var routeManagerReg: ServiceRegistration[RouteManager] = _

  var resourcesTracker: BundleResourcesTracker = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    val classloader = BundleDelegatingClassLoader(ctx)
    val config = ConfigFactory.parseProperties(properties).withFallback(ConfigFactory.load(classloader))
    sprayServer = new SprayServer(config, actorSystem, ctx)
    routeManagerReg = ctx.registerService(classOf[RouteManager], sprayServer, null)
    resourcesTracker = new BundleResourcesTracker(ctx, sprayServer, config.getConfig("spray.can.resources"))
    resourcesTracker.open()
  }

  @Deactivate
  def deactivate: Unit = {
    sprayServer.shutdown()
    routeManagerReg.unregister()
    resourcesTracker.close()
  }

  implicit def toProprties(map: java.util.Map[String, _]): Properties =
    map.keySet().foldLeft(new Properties) { (props, key) ⇒
      map.get(key) match {
        case strVal: String ⇒ props.put(key, strVal)
        case _ ⇒
      }
      props
    }
}