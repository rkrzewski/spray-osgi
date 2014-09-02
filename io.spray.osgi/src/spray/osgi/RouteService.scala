package spray.osgi

import spray.routing.Route

trait RouteService {
  def apply(): Route
}