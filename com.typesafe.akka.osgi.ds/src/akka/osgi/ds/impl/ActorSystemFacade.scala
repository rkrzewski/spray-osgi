package akka.osgi.ds.impl

import org.osgi.framework.BundleContext

import com.typesafe.config.Config

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.Props

object ActorSystemFacadeExtension extends ExtensionId[ActorSystemFacadeProvider] with ExtensionIdProvider {

  def lookup = ActorSystemFacadeExtension

  override def createExtension(system: ExtendedActorSystem) = new ActorSystemFacadeProvider(system)

}

class ActorSystemFacadeProvider(system: ExtendedActorSystem) extends Extension {
  def apply(dynamicConfig: DynamicConfig, context: BundleContext, settings: ActorSystem.Settings) =
    new ActorSystemFacade(system, dynamicConfig, context, settings)
}

class ActorSystemFacade(system: ExtendedActorSystem, dynamicConfig: DynamicConfig, context: BundleContext, val settings: ActorSystem.Settings)
  extends ExtendedActorSystem with Extension {

  def provider: akka.actor.ActorRefProvider =
    system.provider

  def guardian: akka.actor.InternalActorRef =
    system.guardian

  // Members declared in akka.actor.ActorRefFactory   

  def actorOf(props: Props, name: String): akka.actor.ActorRef =
    dynamicConfig.run(context) {
      system.actorOf(Props(classOf[ActorFacade], props, dynamicConfig), name)
    }

  def actorOf(props: akka.actor.Props): akka.actor.ActorRef =
    dynamicConfig.run(context) {
      system.actorOf(Props(classOf[ActorFacade], props, dynamicConfig))
    }

  def stop(actor: akka.actor.ActorRef): Unit =
    system.stop(actor)

  // Members declared in akka.actor.ActorSystem   

  def name: String =
    system.name

  // def settings: akka.actor.ActorSystem.Settings implemented through a val

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
    dynamicConfig.run(context) {
      system.registerExtension(ext)
    }

  def shutdown(): Unit =
    system.shutdown()

  def awaitTermination(): Unit =
    system.awaitTermination()

  def awaitTermination(timeout: scala.concurrent.duration.Duration): Unit =
    system.awaitTermination(timeout)

  def registerOnTermination(code: Runnable): Unit =
    system.registerOnTermination {
      dynamicConfig.run(context)(code)
    }

  def registerOnTermination[T](code: ⇒ T): Unit =
    system.registerOnTermination {
      dynamicConfig.run(context)(code)
    }

  def isTerminated: Boolean =
    system.isTerminated

  def dynamicAccess: akka.actor.DynamicAccess =
    system.dynamicAccess

  def printTree: String =
    system.printTree

  def systemActorOf(props: akka.actor.Props, name: String): akka.actor.ActorRef =
    system.systemActorOf(props, name)

  def systemGuardian: akka.actor.InternalActorRef =
    system.systemGuardian

  def threadFactory: java.util.concurrent.ThreadFactory =
    system.threadFactory

  def lookupRoot: akka.actor.InternalActorRef =
    ???

  def systemImpl: akka.actor.ActorSystemImpl =
    ???
}
