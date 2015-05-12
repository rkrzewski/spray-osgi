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
import org.osgi.service.component.annotations.Reference
import org.osgi.service.component.annotations.ReferenceCardinality
import org.osgi.service.component.annotations.ReferencePolicy
import org.osgi.service.log.LogService
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.osgi.ConfigRecovery

import akka.actor.ActorSystem
import akka.osgi.BundleDelegatingClassLoader

/**
 * A Declarative Services component that provides [[ActorSystemServiceFactory]] with
 * configuration and registers it with the OSGi framework.
 *
 * The component uses `REQUIRE` configuration policy and PID `com.typesafe.akka`. It does not
 * have a method annotated with `@Modified`, which means modification of the configuration
 * causes deactivation of the component and subsequent activation of another instance that will
 * be provided with new configuration.
 */
@Component(
  configurationPid = "com.typesafe.akka",
  configurationPolicy = ConfigurationPolicy.REQUIRE)
class ActorSystemComponent {

  /** Service factory instance. */
  var serviceFactory: Option[ActorSystemServiceFactory] = None

  /** Registration object for the service factory*/
  var registration: Option[ServiceRegistration[_]] = None

  /** OSGi LogService instance */
  var logService: Option[LogService] = None

  /**
   * Invoked by DS runtime when LogService becomes available
   */
  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
  def bindLogService(service: LogService) = {
    logService = Some(service)
    serviceFactory.foreach(_.setLogSevice(logService))
  }

  /**
   * Invoked by DS runtime when LogService disappears
   */
  def unbindLogService(service: LogService) = {
    logService = None
    serviceFactory.foreach(_.setLogSevice(logService))
  }

  /**
   * Starts up the component.
   *
   * At activation, an [[ActorSystemServiceFactory]] will be created and registered
   * with OSGi framework. It will provide `ActorSystem` service objects customized for
   * all requesting bundles.
   *
   * @param ctx `BundleContext` of the `com.typesafe.akka.osgi.ds.impl` bundle
   * @param properties component properties fetched by Declarative Services runtime from
   * `ConfigurationAdmin`.
   */
  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    serviceFactory = Some(new ActorSystemServiceFactory(ConfigRecovery.fromProperties(properties)))
    serviceFactory.foreach(_.setLogSevice(logService))
    registration = serviceFactory.map(ctx.registerService(classOf[ActorSystem].getName(), _, null))
  }

  /**
   * Shuts down the component.
   *
   * At deactivation, all provided service instances will be unregistered, and the `ActorSystem`
   * underlying [[ActorSystemServiceFactory]] will be also shut down.
   */
  @Deactivate
  def deactivate: Unit = {
    registration.foreach(_.unregister())
    serviceFactory.foreach(_.shutdown())
  }
}