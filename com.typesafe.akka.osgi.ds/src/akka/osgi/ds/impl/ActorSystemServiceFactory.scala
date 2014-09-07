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

  val actorBundleContext = new ActorBundleContext(akkaClassLoader, akkaConfig)

  val actorSystem = ActorSystem(actorSystemName,
    Some(actorBundleContext.config),
    Some(actorBundleContext.classLoader),
    None)

  def getService(bundle: Bundle, registration: ServiceRegistration[ActorSystem]): ActorSystem = {
    val bundleContext = bundle.getBundleContext
    val bundleClassLoader = BundleDelegatingClassLoader(bundleContext, None)
    val bundleConfig = ConfigFactory.load(bundleClassLoader)
    actorBundleContext.add(bundleContext, bundleClassLoader, bundleConfig)
    ActorSystemFacadeExtension(actorSystem)(actorBundleContext, bundleContext)
  }

  def ungetService(bundle: Bundle, registration: ServiceRegistration[ActorSystem], actorSystem: ActorSystem): Unit =
    actorBundleContext.remove(bundle.getBundleContext)

  def shutdown(): Unit = {
    val barrier = new CyclicBarrier(2)
    actorSystem.registerOnTermination(barrier.await())
    actorSystem.shutdown()
    barrier.await()
  }

}