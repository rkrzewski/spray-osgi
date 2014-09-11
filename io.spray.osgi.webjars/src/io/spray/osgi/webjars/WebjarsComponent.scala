package io.spray.osgi.webjars

import java.net.URL
import scala.annotation.meta.setter
import scala.collection.JavaConversions._
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.wiring.BundleWire
import org.osgi.framework.wiring.BundleWiring
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference
import org.osgi.resource.Wire
import spray.osgi.BundleResourcesRouteService
import spray.osgi.RouteService
import spray.routing.Route
import spray.routing.Directives.complete
import spray.routing.Directives.path
import spray.routing.Directives.reject
import spray.routing.PathMatcher._
import spray.httpx.marshalling.BasicMarshallers._

@Component
class WebjarsComponent extends RouteService {

  @(Reference @setter)
  var routeService: BundleResourcesRouteService = _

  var requireJs: Option[(Bundle, String)] = None

  @Activate
  def activate(ctx: BundleContext): Unit = {
    val ourWiring = ctx.getBundle.adapt(classOf[BundleWiring])
    val webjarWires: Seq[BundleWire] = ourWiring.getRequiredWires("org.webjars")
    val requireJsBundle = webjarWires match {
      case Seq(requireJsWire) => Some(requireJsWire.getProvider.getBundle)
      case _ => None
    }
    requireJs = for {
      bundle <- requireJsBundle
      val urls = bundle.findEntries("/META-INF/resources", "require.min.js", true)
      url <- if (urls == null || !urls.hasMoreElements()) None else Some(urls.nextElement())
    } yield ((bundle, url.getPath))
  }

  def apply(): Route = {
    requireJs.map { case ((bundle, res)) =>
      path("webjars" / "require.js") {
        routeService.getBundleResource(bundle, res)
      }
    }.getOrElse {
      reject
    }
  }

  @Deactivate
  def deactivate: Unit = {

  }

}