package akka.osgi.ds.impl

import java.util.Properties
import java.util.concurrent.CyclicBarrier

import scala.collection.JavaConversions.asScalaSet
import scala.language.implicitConversions

import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceFactory
import org.osgi.framework.ServiceRegistration
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.ConfigurationPolicy
import org.osgi.service.component.annotations.Deactivate

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.osgi.BundleDelegatingClassLoader
import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

@Component(
  immediate = true,
  configurationPid = "com.typesafe.akka",
  configurationPolicy = ConfigurationPolicy.REQUIRE)
class ActorSystemComponent {

  var serviceFactory: ActorSystemServiceFactory = _

  var registration: ServiceRegistration[_] = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    serviceFactory = new ActorSystemServiceFactory(toConfig(properties))
    registration = ctx.registerService(classOf[ActorSystem].getName(), serviceFactory, null)
  }

  @Deactivate
  def deactivate: Unit = {
    registration.unregister()
    serviceFactory.shutdown()
  }

  def toConfig(map: java.util.Map[String, _]): Config =
    map.keySet().filter(_.endsWith(".origin")).foldLeft(ConfigFactory.empty) { (config, originKey) ⇒
      val key = originKey.replaceAll("\\.origin$", "")
      val originDesc = map.get(originKey).asInstanceOf[String]
      config.withValue(key, ConfigValueFactory.fromAnyRef(map.get(key), originDesc))
    }
}