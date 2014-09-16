package io.spray.osgi.webjars

import java.nio.file.Paths
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

import akka.osgi.ds.ActorService.scalaActorRef
import spray.osgi.BundleResourcesRouteService
import spray.osgi.RouteManager
import spray.routing.Route
import spray.routing.Directive.pimpApply
import spray.routing.Directives.path
import spray.routing.PathMatcher.segmentStringToPathMatcher
import spray.routing.RouteConcatenation.pimpRouteWithConcatenation

/**
 * If require.js webjar is installed in the framework, this component registers
 * shorthand routes to require.js script: {@code /webjars/require.js} and
 * {@code /webjars/require.min.js}.
 *
 * @param requireJsBundle require.js webjar bundle
 * @param ranking the ranking of require.js script routes
 * @param routeService the service building routes for static resources contained in bundles
 * @param routeManager Spray server route manager actor
 */
@Component(configurationPid = "io.spray.webjars")
class RequireJsRouteProvider {

  @(Reference @setter)
  var routeManager: RouteManager = _

  @(Reference @setter)
  var routeService: BundleResourcesRouteService = _

  var route: Option[Route] = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    val ranking = getRanking(properties)
    val ourWiring = ctx.getBundle.adapt(classOf[BundleWiring])
    val webjarWires: Seq[BundleWire] = ourWiring.getRequiredWires("org.webjars")
    val requireJsBundle = webjarWires match {
      case Seq(requireJsWire) => Some(requireJsWire.getProvider.getBundle)
      case _ => None
    }
    route = requireJsBundle.map { bundle =>
      val urls = bundle.findEntries("/META-INF/resources", "*.js", true)
      urls.map { url =>
        val file = Paths.get(url.getPath).getFileName.toString
        path("webjars" / file) {
          routeService.getBundleResource(bundle, url.getPath)
        }
      }.reduceRight(_ ~ _)
    }
    route foreach (routeManager ! RouteManager.RouteAdded(_, ranking))
  }

  private def getRanking(properties: java.util.Map[String, _]): Integer =
    properties.get("spray.webjars.ranking") match {
      case s: String =>
        Integer.parseInt(s)
      case _ =>
        0
    }

  @Deactivate
  def deactivate: Unit = {
    route foreach (routeManager ! RouteManager.RouteRemoved(_))
  }
}