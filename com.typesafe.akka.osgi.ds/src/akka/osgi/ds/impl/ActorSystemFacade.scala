package akka.osgi.ds.impl

import com.typesafe.config.Config
import akka.actor.Actor
import akka.actor.ActorSystem
import org.osgi.framework.BundleContext

class ActorSystemFacade(system: ActorSystem, actorBundleContext: ActorBundleContext, context: BundleContext) extends ActorSystem {

  // Protected members declared in akka.actor.ActorRefFactory, inaccessible

  protected def guardian: akka.actor.InternalActorRef = ???

  protected def lookupRoot: akka.actor.InternalActorRef = ???

  protected def provider: akka.actor.ActorRefProvider = ???

  protected def systemImpl: akka.actor.ActorSystemImpl = ???

  // Members declared in akka.actor.ActorRefFactory   

  def actorOf(props: akka.actor.Props, name: String): akka.actor.ActorRef =
    actorBundleContext.run(context,
      system.actorOf(props, name))

  def actorOf(props: akka.actor.Props): akka.actor.ActorRef =
    actorBundleContext.run(context, system.actorOf(props))

  def stop(actor: akka.actor.ActorRef): Unit =
    system.stop(actor)

  // Members declared in akka.actor.ActorSystem   

  def name: String =
    system.name

  def settings: akka.actor.ActorSystem.Settings =
    system.settings

  def /(name: Iterable[String]): akka.actor.ActorPath =
    system / name

  def /(name: String): akka.actor.ActorPath =
    system / name

  def deadLetters: akka.actor.ActorRef =
    system.deadLetters

  implicit def dispatcher: scala.concurrent.ExecutionContextExecutor =
    system.dispatcher

  def dispatchers: akka.dispatch.Dispatchers =
    system.dispatchers

  def mailboxes: akka.dispatch.Mailboxes =
    system.mailboxes

  def eventStream: akka.event.EventStream =
    system.eventStream

  def scheduler: akka.actor.Scheduler =
    system.scheduler

  def log: akka.event.LoggingAdapter =
    system.log

  def logConfiguration(): Unit =
    system.logConfiguration()

  def extension[T <: akka.actor.Extension](ext: akka.actor.ExtensionId[T]): T =
    system.extension(ext)

  def hasExtension(ext: akka.actor.ExtensionId[_ <: akka.actor.Extension]): Boolean =
    system.hasExtension(ext)

  def registerExtension[T <: akka.actor.Extension](ext: akka.actor.ExtensionId[T]): T =
    actorBundleContext.run(context, system.registerExtension(ext))

  def shutdown(): Unit =
    system.shutdown()

  def awaitTermination(): Unit =
    system.awaitTermination()

  def awaitTermination(timeout: scala.concurrent.duration.Duration): Unit =
    system.awaitTermination(timeout)

  def registerOnTermination(code: Runnable): Unit =
    system.registerOnTermination {
      actorBundleContext.run(context, code)
    }

  def registerOnTermination[T](code: => T): Unit =
    system.registerOnTermination {
      actorBundleContext.run(context, code)
    }

  def isTerminated: Boolean =
    system.isTerminated
}
