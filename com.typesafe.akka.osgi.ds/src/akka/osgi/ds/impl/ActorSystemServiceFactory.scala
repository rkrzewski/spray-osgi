package akka.osgi.ds.impl

import akka.actor.ActorSystem
import org.osgi.framework.ServiceFactory
import akka.osgi.BundleDelegatingClassLoader
import org.osgi.framework.BundleContext
import com.typesafe.config.ConfigFactory
import java.util.Properties
import org.osgi.framework.ServiceRegistration
import org.osgi.framework.Bundle
import java.util.concurrent.CyclicBarrier

class ActorSystemServiceFactory(properties: Properties) extends ServiceFactory[ActorSystem] {

  val akkaClassLoader = classOf[ActorSystem].getClassLoader()

  val akkaConfig = ConfigFactory.parseProperties(properties).withFallback(ConfigFactory.load(akkaClassLoader))

  val actorSystemName = Option(akkaConfig.getString("akka.system-name")).getOrElse("system")

  val dynamicConfig = new DynamicConfig(akkaConfig)

  val actorSystem = ActorSystem(actorSystemName,
    Some(dynamicConfig.config),
    Some(akkaClassLoader),
    None)

  def getService(bundle: Bundle, registration: ServiceRegistration[ActorSystem]): ActorSystem = {
    val bundleContext = bundle.getBundleContext
    val bundleClassLoader = BundleDelegatingClassLoader(bundleContext, None)
    val bundleConfig = ConfigFactory.load(bundleClassLoader)
    dynamicConfig.add(bundleContext, bundleConfig)
    val bundleSettings = new ActorSystem.Settings(bundleClassLoader, bundleConfig, actorSystemName)
    ActorSystemFacadeExtension(actorSystem)(dynamicConfig, bundleContext, bundleSettings)
  }

  def ungetService(bundle: Bundle, registration: ServiceRegistration[ActorSystem], actorSystem: ActorSystem): Unit =
    dynamicConfig.remove(bundle.getBundleContext)

  def shutdown(): Unit = {
    val barrier = new CyclicBarrier(2)
    actorSystem.registerOnTermination(barrier.await())
    actorSystem.shutdown()
    barrier.await()
  }

}