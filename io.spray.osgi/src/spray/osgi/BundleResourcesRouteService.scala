package spray.osgi

import org.osgi.framework.Bundle
import spray.routing.Route

trait BundleResourcesRouteService {

  def getBundleResource(bundle: Bundle, path: String): Route

  def getBundleResources(bundle: Bundle, paths: Seq[String], resBasePath: String): Route
  
}