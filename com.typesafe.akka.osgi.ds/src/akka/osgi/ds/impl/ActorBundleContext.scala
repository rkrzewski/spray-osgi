package akka.osgi.ds.impl

import java.net.URL
import java.util.Enumeration
import akka.osgi.BundleDelegatingClassLoader
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigMergeable
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigOrigin
import com.typesafe.config.ConfigResolveOptions
import com.typesafe.config.ConfigValue
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil

class ActorBundleContext(defaultConfig: Config) {
  
  @volatile
  var contexts: Map[BundleContext, Config] = Map.empty

  def add(bundleContext: BundleContext, config: Config) =
    contexts += bundleContext -> config

  def remove(bundleContext: BundleContext) =
    contexts -= bundleContext

  def run[T](clazz: Class[_], code: => T): T =
    run(FrameworkUtil.getBundle(clazz).getBundleContext, code)

  def run[T](context: BundleContext, code: => T): T =
    run(contexts.getOrElse(context, defaultConfig), code)

  private def run[T](context: Config, code: => T): T =
    try {
      current.set(context)
      code
    } finally {
      current.set(defaultConfig)
    }

  val current = new ThreadLocal[Config] {
    override def initialValue = defaultConfig
  }

  def config = new Config {

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

    /* Note! This is a hack that prevents ActorSystemImpl.Settings from severing ties 
     * with our dynamic Config facility. */
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