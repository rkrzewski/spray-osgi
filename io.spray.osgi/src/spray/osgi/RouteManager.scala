package spray.osgi

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
}

/**
 * An actor service managing Spray Routes that are included into servers configuration.
 *
 * <p>Route references passed in {@code RouteModified} and {@code RouteRemoved} messages must
 * be equal in ({@code eq} sense) to the references passed in {@code RouteAdded} message.</p>
 *
 * <p>Ranking values are arbitrary and fully defined by the application.</p>
 */
object RouteManager {

  /** Base trait of messages understood by {@code RouteManager} */
  sealed trait Message

  /** Sent when a Route is added to server configuration. */
  case class RouteAdded(route: Route, ranking: Int) extends Message

  /** Sent when a Route ranking is changed. */
  case class RouteModified(route: Route, ranking: Int) extends Message

  /** Sent when a Route is removed from server configuration. */
  case class RouteRemoved(route: Route) extends Message

}