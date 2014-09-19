package io.spray.osgi.webjars

import akka.actor.ActorRef

trait RequireJs {
  def ref: ActorRef
}

object RequireJs {
  case class Added(webjar: Webjar)
  case class Removed(webjar: Webjar)
}