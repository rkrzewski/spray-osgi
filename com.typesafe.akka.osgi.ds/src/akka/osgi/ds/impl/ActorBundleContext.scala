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

class ActorBundleContext(defaultClassLoader: ClassLoader, defaultConfig: Config) {
  val defaultContext = (defaultClassLoader, defaultConfig)
  
  @volatile
  var contexts: Map[BundleContext, (ClassLoader, Config)] = Map.empty

  def add(bundleContext: BundleContext, classLoader: ClassLoader, config: Config) =
    contexts += bundleContext -> (classLoader, config)

  def remove(bundleContext: BundleContext) =
    contexts -= bundleContext

  def run[T](clazz: Class[_], code: => T): T =
    run(FrameworkUtil.getBundle(clazz).getBundleContext, code)

  def run[T](context: BundleContext, code: => T): T =
    run(contexts.getOrElse(context, defaultContext), code)

  private def run[T](context: (ClassLoader, Config), code: => T): T =
    try {
      current.set(context)
      code
    } finally {
      current.set(defaultContext)
    }

  val current = new ThreadLocal[(ClassLoader, Config)] {
    override def initialValue = defaultContext
  }

  def classLoader = new ClassLoader {
    override def findClass(name: String): Class[_] =
      current.get._1.loadClass(name)

    override def findResource(name: String): URL =
      current.get._1.getResource(name)

    override def findResources(name: String): Enumeration[URL] =
      current.get._1.getResources(name)
  }

  def config = new Config {

    def atKey(key: String): Config =
      current.get._2.atKey(key)

    def atPath(path: String): Config =
      current.get._2.atPath(path)

    def checkValid(reference: Config, restrictToPaths: java.lang.String*): Unit =
      current.get._2.checkValid(reference, restrictToPaths: _*)

    def entrySet(): java.util.Set[java.util.Map.Entry[String, ConfigValue]] =
      current.get._2.entrySet()

    def getAnyRef(path: String): Object =
      current.get._2.getAnyRef(path)

    def getAnyRefList(path: String): java.util.List[_] =
      current.get._2.getAnyRefList(path)

    def getBoolean(path: String): Boolean =
      current.get._2.getBoolean(path)

    def getBooleanList(path: String): java.util.List[java.lang.Boolean] =
      current.get._2.getBooleanList(path)

    def getBytes(path: String): java.lang.Long =
      current.get._2.getBytes(path)

    def getBytesList(path: String): java.util.List[java.lang.Long] =
      current.get._2.getBytesList(path)

    def getConfig(path: String): Config =
      current.get._2.getConfig(path)

    def getConfigList(path: String): java.util.List[_ <: Config] =
      current.get._2.getConfigList(path)

    def getDouble(path: String): Double =
      current.get._2.getDouble(path)

    def getDoubleList(path: String): java.util.List[java.lang.Double] =
      current.get._2.getDoubleList(path)

    def getDuration(path: String, unit: java.util.concurrent.TimeUnit): Long =
      current.get._2.getDuration(path, unit)

    def getDurationList(path: String, unit: java.util.concurrent.TimeUnit): java.util.List[java.lang.Long] =
      current.get._2.getDurationList(path, unit)

    def getInt(path: String): Int =
      current.get._2.getInt(path)

    def getIntList(path: String): java.util.List[Integer] =
      current.get._2.getIntList(path)

    def getList(path: String): ConfigList =
      current.get._2.getList(path)

    def getLong(path: String): Long =
      current.get._2.getLong(path)

    def getLongList(path: String): java.util.List[java.lang.Long] =
      current.get._2.getLongList(path)

    def getMilliseconds(path: String): java.lang.Long =
      current.get._2.getMilliseconds(path)

    def getMillisecondsList(path: String): java.util.List[java.lang.Long] =
      current.get._2.getMillisecondsList(path)

    def getNanoseconds(path: String): java.lang.Long =
      current.get._2.getNanoseconds(path)

    def getNanosecondsList(path: String): java.util.List[java.lang.Long] =
      current.get._2.getNanosecondsList(path)

    def getNumber(path: String): Number =
      current.get._2.getNumber(path)

    def getNumberList(path: String): java.util.List[Number] =
      current.get._2.getNumberList(path)

    def getObject(path: String): ConfigObject =
      current.get._2.getObject(path)

    def getObjectList(path: String): java.util.List[_ <: ConfigObject] =
      current.get._2.getObjectList(path)

    def getString(path: String): String =
      current.get._2.getString(path)

    def getStringList(path: String): java.util.List[String] =
      current.get._2.getStringList(path)

    def getValue(path: String): ConfigValue =
      current.get._2.getValue(path)

    def hasPath(path: String): Boolean =
      current.get._2.hasPath(path)

    def isEmpty(): Boolean =
      current.get._2.isEmpty()

    def isResolved(): Boolean =
      current.get._2.isResolved()

    def origin(): ConfigOrigin =
      current.get._2.origin()

    def resolve(options: ConfigResolveOptions): Config =
      current.get._2.resolve(options)

    def resolve(): Config =
      current.get._2.resolve()

    def resolveWith(source: Config, options: ConfigResolveOptions): Config =
      current.get._2.resolveWith(source, options)

    def resolveWith(source: Config): Config =
      current.get._2.resolveWith(source)

    def root(): ConfigObject =
      current.get._2.root()

    /* Note! This is a hack that prevents ActorSystemImpl.Settings from severing ties 
     * with our dynamic Config facility. */
    def withFallback(other: ConfigMergeable): Config =
      this

    def withOnlyPath(path: String): Config =
      current.get._2.withOnlyPath(path)

    def withValue(path: String, value: ConfigValue): Config =
      current.get._2.withValue(path, value)

    def withoutPath(path: String): Config =
      current.get._2.withoutPath(path)
  }

}