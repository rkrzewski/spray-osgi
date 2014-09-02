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
class ActorSystemComponent extends ServiceFactory[ActorSystem] {

  var actorSystem: ActorSystem = _

  var registration: ServiceRegistration[_] = _

  var makeFacade: (BundleContext) => ActorSystem = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    val akkaClassLoader = classOf[ActorSystem].getClassLoader()
    val akkaConfig = ConfigFactory.parseProperties(properties).withFallback(ConfigFactory.load(akkaClassLoader))
    val threadContextClassLoader = new ThreadContextClassLoader
    val actorSystemName = Option(akkaConfig.getString("akka.system-name")).getOrElse("system")
    val classloader = BundleDelegatingClassLoader(ctx, Some(threadContextClassLoader))
    val threadContextConfig = new ThreadContextConfig(akkaConfig)
    actorSystem = ActorSystem(actorSystemName, threadContextConfig, classloader)
    makeFacade = {
      bundleContext =>
        val bundleClassLoader = BundleDelegatingClassLoader(bundleContext, None)
        val bundleConfig = ConfigFactory.load(bundleClassLoader) //.withFallback(akkaConfig)
        new OsgiActorSystemFacade(actorSystem,
          threadContextConfig,
          bundleConfig,
          threadContextClassLoader,
          bundleClassLoader)
    }
    registration = ctx.registerService(classOf[ActorSystem].getName(), this, null)
  }

  def getService(bundle: Bundle, registration: ServiceRegistration[ActorSystem]): ActorSystem =
    makeFacade(bundle.getBundleContext())

  def ungetService(bundle: Bundle, registration: ServiceRegistration[ActorSystem], actorSystem: ActorSystem): Unit =
    ()

  @Deactivate
  def deactivate: Unit = {
    registration.unregister()
    val barrier = new CyclicBarrier(2)
    actorSystem.registerOnTermination(barrier.await())
    actorSystem.shutdown()
    barrier.await()
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