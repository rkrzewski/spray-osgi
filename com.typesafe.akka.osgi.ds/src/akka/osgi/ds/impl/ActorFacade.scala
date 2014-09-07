package akka.osgi.ds.impl

import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorCell

class ActorFacade(props: Props, actorBundleContext: ActorBundleContext) extends Actor {
  
  val actor = {
    ActorCell.contextStack.set(context :: Nil)
    props.newActor() 
  }
  
  val clazz = props.clazz

  def receive = actor.receive
  
  override def aroundReceive(receive: Actor.Receive, msg: Any): Unit = 
    actorBundleContext.run(clazz, actor.aroundReceive(actor.receive, msg))
}