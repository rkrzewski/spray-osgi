package spray.osgi.impl

import java.util.concurrent.atomic.AtomicReference

import scala.collection.JavaConversions.enumerationAsScalaIterator

import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.BundleEvent
import org.osgi.util.tracker.BundleTracker

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import spray.osgi.RouteManager
import spray.osgi.RouteManager
import spray.routing.Route

class BundleResourcesTracker(ctx: BundleContext, routeManager: SprayServer)
  extends BundleTracker[AtomicReference[Option[Route]]](ctx, Bundle.ACTIVE, null) {

  import RouteManager._
  val RESOURCES_HEADER = "X-Spray-Resources"

  override def addingBundle(bundle: Bundle, event: BundleEvent): AtomicReference[Option[Route]] = {
    new AtomicReference(makeRoute(bundle).map { route =>
      routeManager.ref ! RouteAdded(route)
      route
    })
  }

  override def removedBundle(bundle: Bundle, event: BundleEvent, webjarRef: AtomicReference[Option[Route]]): Unit = {
    webjarRef.get.foreach(route => routeManager.ref ! RouteRemoved(route))
  }

  override def modifiedBundle(bundle: Bundle, event: BundleEvent, webjarRef: AtomicReference[Option[Route]]): Unit = {
    val route = makeRoute(bundle)
    webjarRef.getAndSet(route).foreach(routeManager.ref ! RouteRemoved(_))
    route.foreach(routeManager.ref ! RouteAdded(_))
  }

  private def makeRoute(bundle: Bundle): Option[Route] = {
    Option(bundle.getHeaders.get(RESOURCES_HEADER)).map(_.trim).map { basePath =>
      val baseURI = bundle.getEntry(basePath).toURI
      val URIs = bundle.findEntries(basePath, "*", true).map(_.toURI).toSeq
      val paths = URIs.map(baseURI.relativize(_).toString)
      routeManager.getBundleResources(bundle, paths, basePath)
    }
  }
}