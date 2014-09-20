package io.spray.osgi.webjars

import scala.annotation.meta.setter
import scala.collection.JavaConversions._
import org.osgi.framework.BundleContext
import org.osgi.framework.Version
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference
import spray.osgi.BundleResourcesRouteService
import spray.osgi.RouteManager
import spray.routing.Route
import org.osgi.framework.Bundle

@Component(configurationPid = "io.spray.webjars")
class WebjarsComponent extends BaseComponent {

  @(Reference @setter)
  var routeService: BundleResourcesRouteService = _

  @(Reference @setter)
  var routeManager: RouteManager = _
  
  @(Reference @setter)
  var requireJs : RequireJs = _

  var tracker: WebjarBundleTracker = _

  var config: Config = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    config = Config(properties)
    tracker = new WebjarBundleTracker(ctx, this)
    tracker.open()
  }

  @Deactivate
  def deactivate: Unit = {
    tracker.close()
  }

  val basePath = "META-INF/resources"

  val webjarPath = basePath + "/webjars"

  private def route(bundle: Bundle): Route = {
    val baseURI = bundle.getEntry(basePath).toURI
    val URIs = bundle.findEntries(basePath, "*", true).map(_.toURI).toSeq
    val paths = URIs.map(baseURI.relativize(_).toString)
    routeService.getBundleResources(bundle, paths, basePath)
  }

  def register(webjar: Webjar): Unit = {
    routeManager.ref ! RouteManager.RouteAdded(route(webjar.bundle), config.ranking)
    requireJs.ref ! RequireJs.Added(webjar)
  }

  def unregister(webjar: Webjar): Unit = {
    routeManager.ref ! RouteManager.RouteRemoved(route(webjar.bundle))
    requireJs.ref ! RequireJs.Removed(webjar)
  }

}