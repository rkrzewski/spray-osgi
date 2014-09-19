package io.spray.osgi.webjars

abstract class BaseComponent {

  case class Config(ranking: Int)

  case object Config {
    def apply(properties: java.util.Map[String, _]) = {
      val ranking = properties.get("spray.webjars.ranking") match {
        case s: String =>
          Integer.parseInt(s)
        case _ =>
          0
      }
      new Config(ranking)
    }
  }

}