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

@Component(
  immediate = true,
  configurationPid = "com.typesafe.akka",
  configurationPolicy = ConfigurationPolicy.REQUIRE)
class ActorSystemComponent {

  var serviceFactory: ActorSystemServiceFactory = _

  var registration: ServiceRegistration[_] = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    serviceFactory = new ActorSystemServiceFactory(properties)
    registration = ctx.registerService(classOf[ActorSystem].getName(), serviceFactory, null)
  }

  @Deactivate
  def deactivate: Unit = {
    registration.unregister()
    serviceFactory.shutdown()
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