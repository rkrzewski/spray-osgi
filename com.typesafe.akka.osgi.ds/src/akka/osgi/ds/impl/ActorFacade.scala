package akka.osgi.ds.impl

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.Duration

import akka.actor.Actor
import akka.actor.ActorCell
import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

/**
 * `ActorFacade` wraps each `Actor` instance created by a client bundle and ensures that
 * appropriate configuration is visible to `receive` and lifecycle methods by
 * wrapping their invocation in [[DynamicConfig.run]].
 *
 * @param props `Props` of the `Actor` the will be wrapped.
 * @param dynamicConfig [[DynamicConfig]] that will be used for configuration switching.
 */
class ActorFacade(props: Props, dynamicConfig: DynamicConfig) extends Actor {

  /**
   * We need to implement our own behavior stack, because the stack in the `ActorCell`
   * of our enclosed `Actor` instance is inaccessible. The stack is initialized with
   * `instance.receive`, so the incoming messages will be passed there through
   * our `aroundRecive` callback.
   */
  private var behaviorStack: List[Actor.Receive] = ActorCell.emptyBehaviorStack

  /** Enclosed `Actor` instance. */
  private val actor = {
    // plug our ActorContextFacade into the context stack of enclosing `ActorCell`
    ActorCell.contextStack.set(new ActorContextFacade(context) :: Nil)
    val instance = props.newActor()
    // initialize the stack with enclosed Actors receive (unless it has already invoked become in 
    // it's constructor body) so the incoming messages will be passed there through our 
    // `aroundRecive` callback
    behaviorStack = if (behaviorStack.isEmpty) instance.receive :: behaviorStack else behaviorStack
    instance
  }

  /** This method is never invoked, messages go straight to the enclosed `Actor`. */
  def receive = {
    case _ ⇒ throw new IllegalStateException("should never be invoked")
  }

  /** Ensures that enclosed `Actor`'s `receive` is executed within `DynamicContext.run`. */
  override def aroundReceive(receive: Actor.Receive, msg: Any): Unit =
    dynamicConfig.run(props.clazz) {
      actor.aroundReceive(behaviorStack.head, msg)
    }

  /** Ensures that enclosed `Actor`'s `preStart` is executed within `DynamicContext.run`. */
  override def aroundPreStart() =
    dynamicConfig.run(props.clazz) {
      actor.aroundPreStart()
    }

  /** Ensures that enclosed `Actor`'s `postStop` is executed within `DynamicContext.run`. */
  override def aroundPostStop() =
    dynamicConfig.run(props.clazz) {
      actor.aroundPostStop()
    }

  /** Ensures that enclosed `Actor`'s `preRestart` is executed within `DynamicContext.run`. */
  override def aroundPreRestart(reason: Throwable, message: Option[Any]) =
    dynamicConfig.run(props.clazz) {
      actor.aroundPreRestart(reason, message)
    }

  /** Ensures that enclosed `Actor`'s `postRestart` is executed within `DynamicContext.run`. */
  override def aroundPostRestart(reason: Throwable) =
    dynamicConfig.run(props.clazz) {
      actor.aroundPostRestart(reason)
    }

  /**
   * `ActorContextFacade` forwards `become`/`unbecome` to `ActorFacade`'s behavior stack,
   * and all other methods to the `ActorContext` associated with `ActorCell` enclosing
   * `ActorFacade`.
   */
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

    // these methods from `ActorRefFactory` need to be present to conform to `ActorContext` type
    // but are never invoked

    def guardian: akka.actor.InternalActorRef = ???

    def lookupRoot: akka.actor.InternalActorRef = ???

    def provider: akka.actor.ActorRefProvider = ???

    def systemImpl: akka.actor.ActorSystemImpl = ???
  }
}
