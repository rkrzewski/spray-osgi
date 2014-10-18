package akka.osgi.ds.impl

import java.net.URL
import java.util.Enumeration

import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil.getBundle

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigMergeable
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigOrigin
import com.typesafe.config.ConfigResolveOptions
import com.typesafe.config.ConfigValue

import akka.osgi.BundleDelegatingClassLoader

/**
 * A configuration switchboard, allowing each client bundle using Akka {@code ActorSystem} access
 * it's own configuration through {@code ActorSystem.settings.config}.
 *
 * <p>
 * {@code ActorSystem} is created by {@link ActorSystemServiceFactory} using {@code Config} object
 * facade provided by this class. Then, for each bundle using {@link ActorSystemServiceFactory} a
 * configuration based on the bundle classpath is created and registered with
 * {@link #add(BundleContext, Config)}. Both {@link ActorSystemFacade} and {@link ActorFacade} wrap
 * calls all client-provided code in {@link run(BundleContext, Function0)} /
 * {@link run(Class, Function0)}, which alter the the thread-local delegate for the
 * facade object to the configuration appropriate for the client bundle.
 * </p>
 *
 * @param default the default configuration, visible to any code accessing the facade that
 * was not invoked using the provided {@code run} methods.
 */
class DynamicConfig(default: Config) {

  /**
   * Registered bundle configurations.
   */
  @volatile
  private var configs: Map[BundleContext, Config] = Map.empty

  /**
   * Register configuration for a bundle.
   *
   * @param bundleContext BundleContext of the bundle.
   * @param config bundle's configuration.
   */
  def add(bundleContext: BundleContext, config: Config) =
    configs += bundleContext → config

  /**
   * Unregister configuration for a bundle.
   *
   * @param bundleContext BundleContext of the bundle.
   */
  def remove(bundleContext: BundleContext) =
    configs -= bundleContext

  /**
   * Execute a block of code, using configuration appropriate for the bundle that the {@code clazz}
   * was loaded from.
   *
   * <p>{@link org.osgi.framework.FrameworkUtil#getBundle(Class)} is used to identify the bundle.</p>
   *
   * @param clazz the Class used to determine which configuration should be used.
   * @param code the code to execute.
   */
  def run[T](clazz: Class[_], code: ⇒ T): T =
    run(configs.getOrElse(getBundle(clazz).getBundleContext, default), code)

  /**
   * Execute a block of code, using configuration appropriate for a given bundle.
   *
   * @param context BundleContext used to determine which configuration should be used.
   * @param code the code to execute.
   */
  def run[T](context: BundleContext, code: ⇒ T): T =
    run(configs.getOrElse(context, default), code)

  /**
   * Execute a block of code, using a specific configuration.
   *
   * @param config the configuration to use.
   * @param code the code to execute.
   */
  private def run[T](config: Config, code: ⇒ T): T =
    try {
      current.set(config)
      code
    } finally {
      current.set(default)
    }

  /**
   * Current delegate for the {@link #config} facade.
   */
  private val current = new ThreadLocal[Config] {
    override def initialValue = default
  }

  /**
   * Configuration facade instance used to initialize ActorSystem.
   *
   * <p>For any code invoked through provided {@code run} methods, it will delegate calls to
   * the configuration appropriate for the bundle from which the code was loaded. For other
   * code (most notably Akka internals themselves) it will delegate calls to the {@link #default}
   * configuration.</p>
   */
  val config: Config = new ConfigFacade

  /**
   * A facade object that delegates all calls, except {@code withFallback(ConfigMergable)} to
   * the thread's {@link #current} configuration.
   */
  class ConfigFacade extends Config {

    def atKey(key: String): Config =
      current.get.atKey(key)

    def atPath(path: String): Config =
      current.get.atPath(path)

    def checkValid(reference: Config, restrictToPaths: java.lang.String*): Unit =
      current.get.checkValid(reference, restrictToPaths: _*)

    def entrySet(): java.util.Set[java.util.Map.Entry[String, ConfigValue]] =
      current.get.entrySet()

    def getAnyRef(path: String): Object =
      current.get.getAnyRef(path)

    def getAnyRefList(path: String): java.util.List[_] =
      current.get.getAnyRefList(path)

    def getBoolean(path: String): Boolean =
      current.get.getBoolean(path)

    def getBooleanList(path: String): java.util.List[java.lang.Boolean] =
      current.get.getBooleanList(path)

    def getBytes(path: String): java.lang.Long =
      current.get.getBytes(path)

    def getBytesList(path: String): java.util.List[java.lang.Long] =
      current.get.getBytesList(path)

    def getConfig(path: String): Config =
      current.get.getConfig(path)

    def getConfigList(path: String): java.util.List[_ <: Config] =
      current.get.getConfigList(path)

    def getDouble(path: String): Double =
      current.get.getDouble(path)

    def getDoubleList(path: String): java.util.List[java.lang.Double] =
      current.get.getDoubleList(path)

    def getDuration(path: String, unit: java.util.concurrent.TimeUnit): Long =
      current.get.getDuration(path, unit)

    def getDurationList(path: String, unit: java.util.concurrent.TimeUnit): java.util.List[java.lang.Long] =
      current.get.getDurationList(path, unit)

    def getInt(path: String): Int =
      current.get.getInt(path)

    def getIntList(path: String): java.util.List[Integer] =
      current.get.getIntList(path)

    def getList(path: String): ConfigList =
      current.get.getList(path)

    def getLong(path: String): Long =
      current.get.getLong(path)

    def getLongList(path: String): java.util.List[java.lang.Long] =
      current.get.getLongList(path)

    def getMilliseconds(path: String): java.lang.Long =
      current.get.getMilliseconds(path)

    def getMillisecondsList(path: String): java.util.List[java.lang.Long] =
      current.get.getMillisecondsList(path)

    def getNanoseconds(path: String): java.lang.Long =
      current.get.getNanoseconds(path)

    def getNanosecondsList(path: String): java.util.List[java.lang.Long] =
      current.get.getNanosecondsList(path)

    def getNumber(path: String): Number =
      current.get.getNumber(path)

    def getNumberList(path: String): java.util.List[Number] =
      current.get.getNumberList(path)

    def getObject(path: String): ConfigObject =
      current.get.getObject(path)

    def getObjectList(path: String): java.util.List[_ <: ConfigObject] =
      current.get.getObjectList(path)

    def getString(path: String): String =
      current.get.getString(path)

    def getStringList(path: String): java.util.List[String] =
      current.get.getStringList(path)

    def getValue(path: String): ConfigValue =
      current.get.getValue(path)

    def hasPath(path: String): Boolean =
      current.get.hasPath(path)

    def isEmpty(): Boolean =
      current.get.isEmpty()

    def isResolved(): Boolean =
      current.get.isResolved()

    def origin(): ConfigOrigin =
      current.get.origin()

    def resolve(options: ConfigResolveOptions): Config =
      current.get.resolve(options)

    def resolve(): Config =
      current.get.resolve()

    def resolveWith(source: Config, options: ConfigResolveOptions): Config =
      current.get.resolveWith(source, options)

    def resolveWith(source: Config): Config =
      current.get.resolveWith(source)

    def root(): ConfigObject =
      current.get.root()

    /**
     * This method always returns {@code this}.
     *
     * <p>
     * This is a hack that prevents {@code ActorSystemImpl.Settings} from severing ties
     * with {@link DynamicConfig facility}. {@code ActorSystemImpl.Settings} call 
     * {@code withFallback} on {@code Config} object provided to to attach default configuration 
     * loaded from Akka's classpath. This simplifies usage, because a blank configuration can be 
     * passed to use default settings. The implementation of the method creates a new configuration 
     * object that bears no relation to it's ancestor configurations.
     * </p>
     *
     * <p>As a consequence of this hack, the facade object can be used as an argument to
     * {@code withFallback} call on another configuration, but will ignore any calls of
     * {@code withFallback} on itself.</p>
     */
    def withFallback(other: ConfigMergeable): Config =
      this

    def withOnlyPath(path: String): Config =
      current.get.withOnlyPath(path)

    def withValue(path: String, value: ConfigValue): Config =
      current.get.withValue(path, value)

    def withoutPath(path: String): Config =
      current.get.withoutPath(path)
  }
}