package test.osgi.actor.impl

import akka.actor.Actor
import test.osgi.actor.api.MyActor

class MyActorImpl extends Actor {
  import MyActor._
  def receive: Receive = {
    case Ping =>
      sender ! Pong
  }
}