package akka.osgi.ds.impl

import java.net.URL
import java.util.Enumeration

class ThreadContextClassLoader extends ClassLoader {

  val current: ThreadLocal[ClassLoader] = new ThreadLocal

  override def findClass(name: String): Class[_] = {
    Option(current.get) match {
      case Some(classLoader) => classLoader.loadClass(name)
      case None => throw new ClassNotFoundException(name)
    }
  }

  override def findResource(name: String): URL = {
    Option(current.get) match {
      case Some(classLoader) => classLoader.getResource(name)
      case None => null
    }
  }

  override def findResources(name: String): Enumeration[URL] = {
    Option(current.get) match {
      case Some(classLoader) => classLoader.getResources(name)
      case None => java.util.Collections.emptyEnumeration()
    }
  }

  def using[T](classLoader: ClassLoader)(code: => T): T = {
    current.set(classLoader)
    try {
      code
    } finally {
      current.set(null)
    }
  }
}
