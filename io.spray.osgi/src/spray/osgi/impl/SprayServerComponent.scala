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

  var sprayServer: SprayServer = _
  
  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    val sprayClassloader = classOf[ServerSettings].getClassLoader
    val classloader = BundleDelegatingClassLoader(ctx, Some(sprayClassloader))
    val config = ConfigFactory.parseProperties(properties).withFallback(ConfigFactory.load(classloader))
    sprayServer = new SprayServer(config, actorSystem, ctx)
  }

  @Deactivate
  def deactivate: Unit = {
    sprayServer.shutdown()
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