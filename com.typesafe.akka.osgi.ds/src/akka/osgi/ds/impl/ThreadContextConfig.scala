package akka.osgi.ds.impl

import com.typesafe.config.Config
import com.typesafe.config.ConfigResolveOptions
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigMergeable
import com.typesafe.config.ConfigOrigin
import com.typesafe.config.ConfigValue

class ThreadContextConfig(baseConfig: Config) extends Config {

  private val current = new ThreadLocal[Config]

  private def config =
    if (current.get != null)
      current.get
    else
      baseConfig

  def using[T](config: Config)(code: => T): T = {
    current.set(config)
    try {
      code
    } finally {
      current.set(null)
    }
  }

  def atKey(key: String): Config =
    config.atKey(key)

  def atPath(path: String): Config =
    config.atPath(path)

  def checkValid(reference: Config, restrictToPaths: java.lang.String*): Unit =
    config.checkValid(reference, restrictToPaths: _*)

  def entrySet(): java.util.Set[java.util.Map.Entry[String, ConfigValue]] =
    config.entrySet()

  def getAnyRef(path: String): Object =
    config.getAnyRef(path)

  def getAnyRefList(path: String): java.util.List[_] =
    config.getAnyRefList(path)

  def getBoolean(path: String): Boolean =
    config.getBoolean(path)

  def getBooleanList(path: String): java.util.List[java.lang.Boolean] =
    config.getBooleanList(path)

  def getBytes(path: String): java.lang.Long =
    config.getBytes(path)

  def getBytesList(path: String): java.util.List[java.lang.Long] =
    config.getBytesList(path)

  def getConfig(path: String): Config =
    config.getConfig(path)

  def getConfigList(path: String): java.util.List[_ <: Config] =
    config.getConfigList(path)

  def getDouble(path: String): Double =
    config.getDouble(path)

  def getDoubleList(path: String): java.util.List[java.lang.Double] =
    config.getDoubleList(path)

  def getDuration(path: String, unit: java.util.concurrent.TimeUnit): Long =
    config.getDuration(path, unit)

  def getDurationList(path: String, unit: java.util.concurrent.TimeUnit): java.util.List[java.lang.Long] =
    config.getDurationList(path, unit)

  def getInt(path: String): Int =
    config.getInt(path)

  def getIntList(path: String): java.util.List[Integer] =
    config.getIntList(path)

  def getList(path: String): ConfigList =
    config.getList(path)

  def getLong(path: String): Long =
    config.getLong(path)

  def getLongList(path: String): java.util.List[java.lang.Long] =
    config.getLongList(path)

  def getMilliseconds(path: String): java.lang.Long =
    config.getMilliseconds(path)

  def getMillisecondsList(path: String): java.util.List[java.lang.Long] =
    config.getMillisecondsList(path)

  def getNanoseconds(path: String): java.lang.Long =
    config.getNanoseconds(path)

  def getNanosecondsList(path: String): java.util.List[java.lang.Long] =
    config.getNanosecondsList(path)

  def getNumber(path: String): Number =
    config.getNumber(path)

  def getNumberList(path: String): java.util.List[Number] =
    config.getNumberList(path)

  def getObject(path: String): ConfigObject =
    config.getObject(path)

  def getObjectList(path: String): java.util.List[_ <: ConfigObject] =
    config.getObjectList(path)

  def getString(path: String): String =
    config.getString(path)

  def getStringList(path: String): java.util.List[String] =
    config.getStringList(path)

  def getValue(path: String): ConfigValue =
    config.getValue(path)

  def hasPath(path: String): Boolean =
    config.hasPath(path)

  def isEmpty(): Boolean =
    config.isEmpty()

  def isResolved(): Boolean =
    config.isResolved()

  def origin(): ConfigOrigin =
    config.origin()

  def resolve(options: ConfigResolveOptions): Config =
    config.resolve(options)

  def resolve(): Config =
    config.resolve()

  def resolveWith(source: Config, options: ConfigResolveOptions): Config =
    config.resolveWith(source, options)

  def resolveWith(source: Config): Config =
    config.resolveWith(source)

  def root(): ConfigObject =
    config.root()

  def withFallback(other: ConfigMergeable): Config =
    this

  def withOnlyPath(path: String): Config =
    config.withOnlyPath(path)

  def withValue(path: String, value: ConfigValue): Config =
    config.withValue(path, value)

  def withoutPath(path: String): Config =
    config.withoutPath(path)
}