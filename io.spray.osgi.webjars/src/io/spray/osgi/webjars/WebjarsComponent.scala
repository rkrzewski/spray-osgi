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

  def loadWebjar(bundle: Bundle): Option[Webjar] = {
    Option(bundle.getEntry(webjarPath)) match {
      case Some(b) =>
        bundle.findEntries(b.getPath, "*", false).toSeq match {
          case Seq(u) =>
            bundle.findEntries(u.getPath, "*", false).toSeq match {
              case Seq(v) =>
                val p = v.getPath.split("/").toSeq.reverse
                Some(Webjar(p(1), new Version(p(0).replace('-', '.')), route(bundle)))
              case _ => None
            }
          case _ => None
        }
      case _ => None
    }
  }

  private def route(bundle: Bundle): Route = {
    val baseURI = bundle.getEntry(basePath).toURI
    val URIs = bundle.findEntries(basePath, "*", true).map(_.toURI).toSeq
    val paths = URIs.map(baseURI.relativize(_).toString)
    routeService.getBundleResources(bundle, paths, basePath)
  }

  def register(webjar: Webjar): Unit =
    routeManager ! RouteManager.RouteAdded(webjar.resourcesRoute, ranking)

  def unregister(webjar: Webjar): Unit =
    routeManager ! RouteManager.RouteRemoved(webjar.resourcesRoute)

}