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
class WebjarsComponent {

  @(Reference @setter)
  var routeService: BundleResourcesRouteService = _

  @(Reference @setter)
  var routeManager: RouteManager = _

  var tracker: WebjarBundleTracker = _

  var ranking: Int = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    ranking = getRanking(properties)
    tracker = new WebjarBundleTracker(ctx, this)
    tracker.open()
  }

  @Deactivate
  def deactivate: Unit = {
    tracker.close()
  }

  private def getRanking(properties: java.util.Map[String, _]): Int =
    properties.get("spray.webjars.ranking") match {
      case s: String =>
        Integer.parseInt(s)
      case _ =>
        0
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
    routeManager ! RouteManager.RouteAdded(route(webjar.bundle), ranking)
  }

  def unregister(webjar: Webjar): Unit = {
    routeManager ! RouteManager.RouteRemoved(route(webjar.bundle))
  }

}