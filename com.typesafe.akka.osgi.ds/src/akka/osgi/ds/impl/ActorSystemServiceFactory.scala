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

/**
 * `ActorSystemServiceFactory` crates custom instances of `ActorSystem` service for requesting
 * bundles.
 *
 * `ServiceFactory` facility is described in chapter 5.6 of OSGi Core specification.
 *
 * @param config user-customized configuration of Akka, used as an override over default
 * configuration loaded from classpath.
 */
class ActorSystemServiceFactory(config: Config) extends ServiceFactory[ActorSystem] {

  /** The `ClassLoader` of `akka-actor` bundle. */
  private val akkaClassLoader = classOf[ActorSystem].getClassLoader()

  /** Combined Akka coniguration: user overrides with fallback to classpath. */
  private val akkaConfig = config.withFallback(ConfigFactory.load(akkaClassLoader)).resolve

  /**
   * System actor name. Default value `system` should do just fine, because the purpose
   *  of `ActorSystemServiceFactory` is to use single `ActorSystem` instance per JVM.
   */
  private val actorSystemName = Option(akkaConfig.getString("akka.system-name")).getOrElse("system")

  /**
   * The [[DynamicConfig]] instance used for configuration switching, initialized with
   * combined Akka configuration.
   */
  private val dynamicConfig = new DynamicConfig(akkaConfig)

  /**
   * The underlying `ActorSystem` instance.
   */
  private val actorSystem = ActorSystem(actorSystemName,
    Some(dynamicConfig.config),
    Some(akkaClassLoader),
    None)

  /**
   * Provides a custom `ActorSystem` service to a bundle.
   *
   * @param bundle the requesting a service instance.
   * @param registration the service registration token. Makes it possible to invalidate the
   * service instance at the provider's discretion.
   * @return `ActorSystemFacade` instance what will ensure that the bundle can access it's
   * configuration defined on classpath through `ActorSystem.settings.config`.
   */
  def getService(bundle: Bundle, registration: ServiceRegistration[ActorSystem]): ActorSystem = {
    val bundleContext = bundle.getBundleContext
    val bundleClassLoader = BundleDelegatingClassLoader(bundleContext, None)
    val bundleConfig = ConfigFactory.load(bundleClassLoader)
    dynamicConfig.add(bundleContext, bundleConfig)
    val bundleSettings = new ActorSystem.Settings(bundleClassLoader, bundleConfig, actorSystemName)
    ActorSystemFacade.Extension(actorSystem)(dynamicConfig, bundleContext, bundleSettings)
  }

  /**
   * Called when requester bundle releases the service reference.
   * 
   * @param bundle that has requested a `ActorSystem` service instance.
   * @param registration the service registration token.
   * @param actorSstem the service instance.
   */
  def ungetService(bundle: Bundle, registration: ServiceRegistration[ActorSystem], actorSystem: ActorSystem): Unit =
    dynamicConfig.remove(bundle.getBundleContext)

  def shutdown(): Unit = {
    actorSystem.shutdown()
    actorSystem.awaitTermination()
  }

}