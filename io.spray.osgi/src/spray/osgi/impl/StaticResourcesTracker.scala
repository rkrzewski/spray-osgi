package spray.osgi.impl

import scala.collection.JavaConversions.enumerationAsScalaIterator
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.BundleEvent
import org.osgi.framework.startlevel.BundleStartLevel
import org.osgi.util.tracker.BundleTracker
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import spray.routing.Route
import org.osgi.framework.wiring.BundleWiring
import akka.actor.ActorSystem
import java.util.concurrent.atomic.AtomicReference

class StaticResourcesTracker(ctx: BundleContext, routeManager: ActorRef)(implicit val actorSystem: ActorSystem)
  extends BundleTracker[AtomicReference[Option[Route]]](ctx, Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE, null) {

  import RouteManager._
  import StaticResourcesDirective._

  val basePath = "/META-INF/resources"

  override def addingBundle(bundle: Bundle, event: BundleEvent): AtomicReference[Option[Route]] = {
    val routeOption = route(bundle)
    val ranking = -bundle.adapt(classOf[BundleStartLevel]).getStartLevel()
    routeOption foreach (routeManager ! RouteAdded(_, ranking))
    new AtomicReference(routeOption)
  }

  override def removedBundle(bundle: Bundle, event: BundleEvent, holder: AtomicReference[Option[Route]]) {
    holder.get foreach (routeManager ! RouteRemoved(_))
  }

  override def modifiedBundle(bundle: Bundle, event: BundleEvent, holder: AtomicReference[Option[Route]]) {
    val newRouteOption = route(bundle)
    val newRanking = -bundle.adapt(classOf[BundleStartLevel]).getStartLevel()
    val oldRouteOption = holder.getAndSet(newRouteOption)
    oldRouteOption foreach (routeManager ! RouteRemoved(_))
    newRouteOption foreach (routeManager ! RouteAdded(_, newRanking))
  }

  def route(bundle: Bundle): Option[Route] =
    Option(bundle.getEntry(basePath)) map (_.toURI) map { baseURI =>
      val URIs = bundle.findEntries(basePath, "*", true).map(_.toURI).toSeq
      val paths = URIs.map(baseURI.relativize(_).toString)
      getStaticResource(paths, basePath, bundle)
    }
}