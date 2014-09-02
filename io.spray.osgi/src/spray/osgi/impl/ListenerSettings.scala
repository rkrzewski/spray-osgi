package spray.osgi.impl

import scala.collection.immutable
import akka.io.Inet
import akka.io.Tcp
import com.typesafe.config.Config
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

case class ListenerSettings(
  interface: String,
  port: Int,
  backlog: Int,
  bindTimeout: FiniteDuration,
  unbindTimeout: FiniteDuration,
  socketOptions: immutable.Traversable[Inet.SocketOption] = Nil)

object ListenerSettings {
  
  implicit class ConfigWithDuration(c: Config) {
    def getDuration(name: String): FiniteDuration = 
      FiniteDuration(c.getDuration(name, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }
  
  def fromSubConfig(c: Config) = apply(
    c getString "interface",
    c getInt "port",
    c getInt "backlog",
    c getDuration "bind-timeout",
    c getDuration "unbind-timeout",
    socketOptions(c))

  def socketOptions(c: Config): List[Inet.SocketOption] = {
    val b = ListBuffer[Inet.SocketOption]()
    if (c.hasPath("socket-config")) {
      val sc = c.getConfig("socket-config")

      if (sc.hasPath("receive-buffer-size"))
        b += Inet.SO.ReceiveBufferSize(sc.getInt("receive-buffer-size"))
      if (sc.hasPath("send-buffer-size"))
        b += Inet.SO.SendBufferSize(sc.getInt("send-buffer-size"))
      if (sc.hasPath("traffic-class"))
        b += Inet.SO.TrafficClass(sc.getInt("trafic-class"))
      if (sc.hasPath("reuse-address"))
        b += Inet.SO.ReuseAddress(sc.getBoolean("reuse-address"))

      if (sc.hasPath("keep-alive"))
        b += Tcp.SO.KeepAlive(sc.getBoolean("keep-alive"))
      if (sc.hasPath("oob-inline"))
        b += Tcp.SO.OOBInline(sc.getBoolean("oob-inline"))
      if (sc.hasPath("tcp-no-delay"))
        b += Tcp.SO.OOBInline(sc.getBoolean("tcp-no-delay"))
    }
    b.toList
  }
}