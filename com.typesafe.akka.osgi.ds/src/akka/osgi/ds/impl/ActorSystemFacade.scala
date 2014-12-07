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

/**
 * `ActorSystemFacade` companion object facilitates `ActorSystemFacade` initialization
 * by plugging into Akka as an [[akka.actor.Extension]].
 */
object ActorSystemFacade {

  /**
   * Akka Extension used to gain access to `ExtendedActorSystem` needed to implement the
   * functionality of [[ActorSystemFacade!]]
   */
  object Extension extends ExtensionId[Provider] with ExtensionIdProvider {

    /**
     * @return `Extension` object instance.
     */
    def lookup = Extension

    /**
     * @return a [[Provider]] instance.
     */
    override def createExtension(system: ExtendedActorSystem) = new Provider(system)
  }

  /**
   * Helper class for `ActorSystemFacade.Extension` that instantiates `ActorSystemFacade`
   *
   * Note that the unqualified name `Extension` in extends clause refers to `akka.actor.Extension`.
   */
  class Provider(system: ExtendedActorSystem) extends Extension {

    /**
     * Instantiates a `ActorSystemFacade`.
     *
     * @param dynamicConfig [[DynamicConfig]] that will be used for configuration switching.
     * @param context `BundleContext` of the client bundle.
     * @param settings Akka settings built according to client bundle's classpath.
     */
    def apply(dynamicConfig: DynamicConfig, context: BundleContext, settings: ActorSystem.Settings) =
      new ActorSystemFacade(system, dynamicConfig, context, settings)
  }
}

/**
 * `ActorSystemFacade` is the view of `ActorSystem` provided by `ActorSystemServiceFactory` to
 * client bundles.
 *
 * @param system the wrapped [[akka.actor.ActorSystem]]
 * @param dynamicConfig [[DynamicConfig]] that will be used for configuration switching.
 * @param context `BundleContext` of the client bundle.
 * @param settings Akka settings built according to client bundle's classpath.
 */
class ActorSystemFacade(system: ExtendedActorSystem, dynamicConfig: DynamicConfig, context: BundleContext, val settings: ActorSystem.Settings)
  extends ExtendedActorSystem {

  // Members declared in akka.actor.ActorRefFactory that cannot be implemented by delegation

  def systemImpl: akka.actor.ActorSystemImpl =
    ???

  def lookupRoot: akka.actor.InternalActorRef =
    ???

  // Members declared in akka.actor.ActorRefFactory   

  def provider: akka.actor.ActorRefProvider =
    system.provider

  implicit def dispatcher: scala.concurrent.ExecutionContextExecutor =
    system.dispatcher

  def guardian: akka.actor.InternalActorRef =
    system.guardian

  /**
   * `ActorSystemFacade` wraps each `Actor` created by the client bundle into
   * `ActorFacade` to ensure that appropriate configuration is accessible during execution of
   * message processing and lifecycle methods of the actor.
   */
  def actorOf(props: akka.actor.Props): akka.actor.ActorRef =
    dynamicConfig.run(context) {
      system.actorOf(Props(classOf[ActorFacade], props, dynamicConfig))
    }

  /**
   * `ActorSystemFacade` wraps each `Actor` created by the client bundle into
   * `ActorFacade` to ensure that appropriate configuration is accessible during execution of
   * message processing and lifecycle methods of the actor.
   */
  def actorOf(props: Props, name: String): akka.actor.ActorRef =
    dynamicConfig.run(context) {
      system.actorOf(Props(classOf[ActorFacade], props, dynamicConfig), name)
    }

  def stop(actor: akka.actor.ActorRef): Unit =
    system.stop(actor)

  // Members declared in akka.actor.ActorSystem   

  def name: String =
    system.name

  // def settings: akka.actor.ActorSystem.Settings implemented through a val

  def logConfiguration(): Unit =
    system.logConfiguration()

  def /(name: Iterable[String]): akka.actor.ActorPath =
    system / name

  def /(name: String): akka.actor.ActorPath =
    system / name

  def eventStream: akka.event.EventStream =
    system.eventStream

  def log: akka.event.LoggingAdapter =
    system.log

  def deadLetters: akka.actor.ActorRef =
    system.deadLetters

  def scheduler: akka.actor.Scheduler =
    system.scheduler

  def dispatchers: akka.dispatch.Dispatchers =
    system.dispatchers

  def mailboxes: akka.dispatch.Mailboxes =
    system.mailboxes

  /**
   * `ActorSystemFacade` wraps the provided code in [[DynamicConfig.run]]
   */
  def registerOnTermination[T](code: ⇒ T): Unit =
    system.registerOnTermination {
      dynamicConfig.run(context)(code)
    }

  /**
   * `ActorSystemFacade` wraps the provided code in [[DynamicConfig.run]]
   */
  def registerOnTermination(code: Runnable): Unit =
    system.registerOnTermination {
      dynamicConfig.run(context)(code)
    }

  def awaitTermination(timeout: scala.concurrent.duration.Duration): Unit =
    system.awaitTermination(timeout)

  def awaitTermination(): Unit =
    system.awaitTermination()

  def shutdown(): Unit =
    system.shutdown()

  def isTerminated: Boolean =
    system.isTerminated

  /**
   * `ActorSystemFacade` wraps extension initialization in [[DynamicConfig.run]] 
   */
  def registerExtension[T <: akka.actor.Extension](ext: akka.actor.ExtensionId[T]): T =
    dynamicConfig.run(context) {
      system.registerExtension(ext)
    }

  def extension[T <: akka.actor.Extension](ext: akka.actor.ExtensionId[T]): T =
    system.extension(ext)

  def hasExtension(ext: akka.actor.ExtensionId[_ <: akka.actor.Extension]): Boolean =
    system.hasExtension(ext)

  // Members declared in akka.actor.ExtendedActorSystem  

  def systemGuardian: akka.actor.InternalActorRef =
    system.systemGuardian

  def systemActorOf(props: akka.actor.Props, name: String): akka.actor.ActorRef =
    system.systemActorOf(props, name)

  def threadFactory: java.util.concurrent.ThreadFactory =
    system.threadFactory

  def dynamicAccess: akka.actor.DynamicAccess =
    system.dynamicAccess

  def printTree: String =
    system.printTree
}
