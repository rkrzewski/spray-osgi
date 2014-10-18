package akka.osgi.ds.impl

import java.util.concurrent.CyclicBarrier
import java.util.Properties

import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceFactory
import org.osgi.framework.ServiceRegistration

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.osgi.BundleDelegatingClassLoader

class ActorSystemServiceFactory(config: Config) extends ServiceFactory[ActorSystem] {

  val akkaClassLoader = classOf[ActorSystem].getClassLoader()

  val akkaConfig = config.withFallback(ConfigFactory.load(akkaClassLoader)).resolve

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
    actorSystem.shutdown()
    actorSystem.awaitTermination()
  }

}