package akka.osgi.ds

import akka.actor.ActorRef
import akka.actor.ScalaActorRef

/**
 * A marker trait for OSGi service implemented as Akka actors.
 *
 * <p>Applications may subclass this trait, and register the instances of subclass into OSGi
 * framework to advertise their functionality implemented using Akka actors to other components.
 * This is preferred to publishing an {@code ActorRef} as service directly, because the clients
 * wouldn't be able to track the specific service using it's class, but the provider and the
 * clients would need to agree on some additional service properties instead.</p>
 *
 * <p>The subclass of {@code ActorService} should be placed in the exported API package of the
 * component providing the service, along with a companion object defining supported messages.<p>
 */
trait ActorService {

  /** Returns the ActorRef for the actor performing the actual service. */
  def apply(): ActorRef
}

/** 
 * Companion object for the {@code ActorService} trait providing implicit conversions allowing 
 * using instances of the actor service with syntax typical for an {@code ActorRef}.  
 */
object ActorService {

  /** Allows using {@code !} operator on {@code ActorService} instance. */
  implicit def scalaActorRef(service: ActorService): ScalaActorRef =
    service()

  /** Allows using {@code ?} operator on {@code ActorService} instance. */
  implicit def ask(service: ActorService) =
    akka.pattern.ask(service())
}