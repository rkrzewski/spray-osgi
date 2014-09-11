package spray.osgi.impl

import spray.osgi.BundleResourcesRouteService
import org.osgi.service.component.annotations.Component
import akka.actor.ActorSystem
import scala.annotation.meta.setter
import org.osgi.service.component.annotations.Reference
import spray.routing.Route
import org.osgi.framework.Bundle

@Component
class BundleResourcesRouteComponent extends BundleResourcesRouteService {

  @(Reference @setter)
  implicit var actorSystem: ActorSystem = _

  def getBundleResource(bundle: Bundle, path: String): Route = {
    StaticResourcesDirective.getBundleResource(bundle, path)
  }

  def getBundleResources(bundle: Bundle, paths: Seq[String], resBasePath: String): Route = {
    StaticResourcesDirective.getBundleResources(bundle, paths, resBasePath)
  }

}