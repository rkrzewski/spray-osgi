package spray.osgi

import spray.routing.Route

/**
 * An OSGi service that provides a Spray route that should be included in the servers configuration.
 *
 * <p>Spray server Declarative Services component is tracking services of this type and adjusts the
 * configuration appropriately. {@link org.osgi.framework.Constants.SERVICE_RANKING} service property
 * values will be used for order of precedence of available routes.</p>
 */
trait RouteProvider {

  /** Returns a route definition */
  def route: Route

}