package akka.osgi.ds.impl

import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorCell
import akka.japi.Procedure
import scala.concurrent.ExecutionContextExecutor
import akka.actor.ActorSystem
import scala.concurrent.duration.Duration
import akka.actor.ActorRef

class ActorFacade(props: Props, actorBundleContext: ActorBundleContext) extends Actor {

  private var behaviorStack: List[Actor.Receive] = ActorCell.emptyBehaviorStack

  private val actor = {
    ActorCell.contextStack.set(new ActorContextFacade(context) :: Nil)
    val instance = props.newActor()
    behaviorStack = if (behaviorStack.isEmpty) instance.receive :: behaviorStack else behaviorStack
    instance
  }

  private val clazz = props.clazz

  def receive = { 
    case _ => throw new IllegalStateException("should never be invoked")
  }

  override def aroundReceive(receive: Actor.Receive, msg: Any): Unit =
    actorBundleContext.run(clazz, actor.aroundReceive(behaviorStack.head, msg))

  class ActorContextFacade(context: ActorContext) extends ActorContext {

    def become(behavior: Actor.Receive, discardOld: Boolean = true): Unit =
      behaviorStack = behavior :: (if (discardOld && behaviorStack.nonEmpty) behaviorStack.tail else behaviorStack)

    def unbecome(): Unit = {
      val original = behaviorStack
      behaviorStack =
        if (original.isEmpty || original.tail.isEmpty) actor.receive :: ActorCell.emptyBehaviorStack
        else original.tail
    }

    def self: ActorRef = context.self

    def props: Props = context.props

    def receiveTimeout: Duration = context.receiveTimeout

    def setReceiveTimeout(timeout: Duration): Unit = context.setReceiveTimeout(timeout)

    def sender(): ActorRef = context.sender()

    def children: scala.collection.immutable.Iterable[ActorRef] = context.children

    def child(name: String): Option[ActorRef] = context.child(name)

    implicit def dispatcher: ExecutionContextExecutor = context.dispatcher

    implicit def system: ActorSystem = context.system

    def parent: ActorRef = context.parent

    def watch(subject: ActorRef): ActorRef = context.watch(subject)

    def unwatch(subject: ActorRef): ActorRef = context.unwatch(subject)

    def actorOf(props: Props): ActorRef = context.actorOf(props)

    def actorOf(props: Props, name: String): ActorRef = context.actorOf(props, name)

    def stop(actor: akka.actor.ActorRef): Unit = context.stop(actor)

    def guardian: akka.actor.InternalActorRef = ???

    def lookupRoot: akka.actor.InternalActorRef = ???

    def provider: akka.actor.ActorRefProvider = ???

    def systemImpl: akka.actor.ActorSystemImpl = ???
  }
}
