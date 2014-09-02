package test.osgi.actor.api

import akka.osgi.ds.ActorService

trait MyActor extends ActorService

object MyActor {
  case object Ping
  case object Pong
}