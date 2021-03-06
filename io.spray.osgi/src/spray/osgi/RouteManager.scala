package spray.osgi

import org.osgi.framework.Bundle

import spray.routing.Route
import akka.actor.ActorRef

/**
 * An service managing Spray routes that are included into servers configuration.
 */
trait RouteManager {

  /**
   * Reference to actor managing Spray routes.
   *
   * <p>See companion object documentation for supported messages.</p>
   */
  def ref: ActorRef

  def getBundleResource(bundle: Bundle, path: String): Route

  def getBundleResources(bundle: Bundle, paths: Seq[String], resBasePath: String): Route
}

/**
 * An actor service managing Spray Routes that are included into servers configuration.
 *
 * <p>Route references passed in {@code RouteRemoved} messages must be equal in 
 * ({@code eq} sense) to the references passed in {@code RouteAdded} message.</p>
 */
object RouteManager {

  /** Base trait of messages understood by {@code RouteManager} */
  sealed trait Message

  /** Sent when a Route is added to server configuration. */
  case class RouteAdded(route: Route) extends Message

  /** Sent when a Route is removed from server configuration. */
  case class RouteRemoved(route: Route) extends Message

}