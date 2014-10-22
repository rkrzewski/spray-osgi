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

  /**
   * INTERNAL API
   */
  def systemImpl: akka.actor.ActorSystemImpl =
    ???

  /**
   * INTERNAL API
   */
  def lookupRoot: akka.actor.InternalActorRef =
    ???

  // Members declared in akka.actor.ActorRefFactory   

  /**
   * The ActorRefProvider is the only entity which creates all actor references within this actor 
   * system.
   */
  def provider: akka.actor.ActorRefProvider =
    system.provider

  /**
   * Returns the default MessageDispatcher associated with this ActorRefFactory
   */
  implicit def dispatcher: scala.concurrent.ExecutionContextExecutor =
    system.dispatcher

  /**
   * The top-level supervisor of all actors created using system.actorOf(...).
   */
  def guardian: akka.actor.InternalActorRef =
    system.guardian

  /**
   * Create new actor as child of this context and give it an automatically
   * generated name (currently similar to base64-encoded integer count,
   * reversed and with “$” prepended, may change in the future).
   *
   * See [[akka.actor.Props]] for details on how to obtain a `Props` object.
   *
   * '''Note''' `ActorSystemFacade` wraps each `Actor` created by the client bundle into
   * `ActorFacade` to ensure that appropriate configuration is accessible during execution of
   * message processing and lifecycle methods of the actor.
   *
   * @throws akka.ConfigurationException if deployment, dispatcher
   *   or mailbox configuration is wrong
   */
  def actorOf(props: akka.actor.Props): akka.actor.ActorRef =
    dynamicConfig.run(context) {
      system.actorOf(Props(classOf[ActorFacade], props, dynamicConfig))
    }

  /**
   * Create new actor as child of this context with the given name, which must
   * not be null, empty or start with “$”. If the given name is already in use,
   * an `InvalidActorNameException` is thrown.
   *
   * '''Note''' `ActorSystemFacade` wraps each `Actor` created by the client bundle into
   * `ActorFacade` to ensure that appropriate configuration is accessible during execution of
   * message processing and lifecycle methods of the actor.
   *
   * See [[akka.actor.Props]] for details on how to obtain a `Props` object.
   * @throws akka.actor.InvalidActorNameException if the given name is
   *   invalid or already in use
   * @throws akka.ConfigurationException if deployment, dispatcher
   *   or mailbox configuration is wrong
   */
  def actorOf(props: Props, name: String): akka.actor.ActorRef =
    dynamicConfig.run(context) {
      system.actorOf(Props(classOf[ActorFacade], props, dynamicConfig), name)
    }

  /**
   * Stop the actor pointed to by the given [[akka.actor.ActorRef]]; this is
   * an asynchronous operation, i.e. involves a message send.
   */
  def stop(actor: akka.actor.ActorRef): Unit =
    system.stop(actor)

  // Members declared in akka.actor.ActorSystem   

  /**
   * The name of this actor system, used to distinguish multiple ones within
   * the same JVM & class loader.
   */
  def name: String =
    system.name

  // def settings: akka.actor.ActorSystem.Settings implemented through a val

  /**
   * Log the configuration.
   */
  def logConfiguration(): Unit =
    system.logConfiguration()

  /**
   * Construct a path below the application guardian to be used with [[ActorSystem.actorSelection]].
   */
  def /(name: Iterable[String]): akka.actor.ActorPath =
    system / name

  /**
   * Construct a path below the application guardian to be used with [[ActorSystem.actorSelection]].
   */
  def /(name: String): akka.actor.ActorPath =
    system / name

  /**
   * Main event bus of this actor system, used for example for logging.
   */
  def eventStream: akka.event.EventStream =
    system.eventStream

  /**
   * Convenient logging adapter for logging to the [[ActorSystem.eventStream]].
   */
  def log: akka.event.LoggingAdapter =
    system.log

  /**
   * Actor reference where messages are re-routed to which were addressed to
   * stopped or non-existing actors. Delivery to this actor is done on a best
   * effort basis and hence not strictly guaranteed.
   */
  def deadLetters: akka.actor.ActorRef =
    system.deadLetters

  /**
   * Light-weight scheduler for running asynchronous tasks after some deadline
   * in the future. Not terribly precise but cheap.
   */
  def scheduler: akka.actor.Scheduler =
    system.scheduler

  /**
   * Helper object for looking up configured dispatchers.
   */
  def dispatchers: akka.dispatch.Dispatchers =
    system.dispatchers

  /**
   * Helper object for looking up configured mailbox types.
   */
  def mailboxes: akka.dispatch.Mailboxes =
    system.mailboxes

  /**
   * Register a block of code (callback) to run after ActorSystem.shutdown has been issued and
   * all actors in this actor system have been stopped.
   * Multiple code blocks may be registered by calling this method multiple times.
   * The callbacks will be run sequentially in reverse order of registration, i.e.
   * last registration is run first.
   * 
   * '''Note''' `ActorSystemFacade` wraps the provided code in [[DynamicConfig.run]]
   *
   * @throws a RejectedExecutionException if the System has already shut down or if shutdown has
   * been initiated.
   *
   * Scala API
   */
  def registerOnTermination[T](code: ⇒ T): Unit =
    system.registerOnTermination {
      dynamicConfig.run(context)(code)
    }

  /**
   * Java API: Register a block of code (callback) to run after ActorSystem.shutdown has been
   * issued and all actors in this actor system have been stopped.
   * Multiple code blocks may be registered by calling this method multiple times.
   * The callbacks will be run sequentially in reverse order of registration, i.e.
   * last registration is run first.
   * 
   * '''Note''' `ActorSystemFacade` wraps the provided code in [[DynamicConfig.run]]
   *
   * @throws a RejectedExecutionException if the System has already shut down or if shutdown has
   * been initiated.
   */
  def registerOnTermination(code: Runnable): Unit =
    system.registerOnTermination {
      dynamicConfig.run(context)(code)
    }

  /**
   * Block current thread until the system has been shutdown, or the specified
   * timeout has elapsed. This will block until after all on termination
   * callbacks have been run.
   *
   * @throws TimeoutException in case of timeout
   */
  def awaitTermination(timeout: scala.concurrent.duration.Duration): Unit =
    system.awaitTermination(timeout)

  /**
   * Register a block of code (callback) to run after ActorSystem.shutdown has been issued and
   * all actors in this actor system have been stopped.
   * Multiple code blocks may be registered by calling this method multiple times.
   * The callbacks will be run sequentially in reverse order of registration, i.e.
   * last registration is run first.
   *
   * @throws a RejectedExecutionException if the System has already shut down or if shutdown has
   * been initiated.
   *
   * Scala API
   */
  def awaitTermination(): Unit =
    system.awaitTermination()

  /**
   * Stop this actor system. This will stop the guardian actor, which in turn
   * will recursively stop all its child actors, then the system guardian
   * (below which the logging actors reside) and the execute all registered
   * termination handlers (see [[ActorSystem.registerOnTermination]]).
   */
  def shutdown(): Unit =
    system.shutdown()

  /**
   * Query the termination status: if it returns true, all callbacks have run
   * and the ActorSystem has been fully stopped, i.e.
   * `awaitTermination(0 seconds)` would return normally. If this method
   * returns `false`, the status is actually unknown, since it might have
   * changed since you queried it.
   */
  def isTerminated: Boolean =
    system.isTerminated

  /**
   * Registers the provided extension and creates its payload, if this extension isn't already
   * registered. This method has putIfAbsent-semantics, this method can potentially block, waiting
   * for the initialization of the payload, if is in the process of registration from another
   * Thread of execution.
   * 
   * '''Note''' `ActorSystemFacade` wraps extension initialization in [[DynamicConfig.run]] 
   */
  def registerExtension[T <: akka.actor.Extension](ext: akka.actor.ExtensionId[T]): T =
    dynamicConfig.run(context) {
      system.registerExtension(ext)
    }

  /**
   * Returns the payload that is associated with the provided extension
   * throws an IllegalStateException if it is not registered.
   * This method can potentially block, waiting for the initialization
   * of the payload, if is in the process of registration from another Thread of execution
   */
  def extension[T <: akka.actor.Extension](ext: akka.actor.ExtensionId[T]): T =
    system.extension(ext)

  /**
   * Returns whether the specified extension is already registered, this method can potentially
   * block, waiting for the initialization of the payload, if is in the process of registration 
   * from another Thread of execution
   */
  def hasExtension(ext: akka.actor.ExtensionId[_ <: akka.actor.Extension]): Boolean =
    system.hasExtension(ext)

  // Member declared in akka.actor.ExtendedActorSystem  

  /**
   * The top-level supervisor of all system-internal services like logging.
   */
  def systemGuardian: akka.actor.InternalActorRef =
    system.systemGuardian

  /**
   * Create an actor in the "/system" namespace. This actor will be shut down
   * during system shutdown only after all user actors have terminated.
   */
  def systemActorOf(props: akka.actor.Props, name: String): akka.actor.ActorRef =
    system.systemActorOf(props, name)

  /**
   * A ThreadFactory that can be used if the transport needs to create any Threads
   */
  def threadFactory: java.util.concurrent.ThreadFactory =
    system.threadFactory

  /**
   * ClassLoader wrapper which is used for reflective accesses internally. This is set
   * to use the context class loader, if one is set, or the class loader which
   * loaded the ActorSystem implementation. The context class loader is also
   * set on all threads created by the ActorSystem, if one was set during
   * creation.
   */
  def dynamicAccess: akka.actor.DynamicAccess =
    system.dynamicAccess

  /**
   * For debugging: traverse actor hierarchy and make string representation.
   * Careful, this may OOM on large actor systems, and it is only meant for
   * helping debugging in case something already went terminally wrong.
   */
  def printTree: String =
    system.printTree
}
