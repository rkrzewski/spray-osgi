package spray.osgi.impl

import java.util.concurrent.atomic.AtomicReference

import scala.collection.JavaConversions.enumerationAsScalaIterator

import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.BundleEvent
import org.osgi.util.tracker.BundleTracker

import com.typesafe.config.Config

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import spray.osgi.RouteManager
import spray.routing.Directives.path
import spray.routing.Route
import spray.routing.RouteConcatenation._

class BundleResourcesTracker(ctx: BundleContext, routeManager: SprayServer, config: Config)
  extends BundleTracker[AtomicReference[Option[Route]]](ctx, Bundle.ACTIVE, null) {

  val basePath = config.getString("directory")
  val welcomeFile = config.getString("welcome-file")

  import RouteManager._

  override def addingBundle(bundle: Bundle, event: BundleEvent): AtomicReference[Option[Route]] = {
    new AtomicReference(makeRoute(bundle).map { route ⇒
      routeManager.ref ! RouteAdded(route)
      route
    })
  }

  override def removedBundle(bundle: Bundle, event: BundleEvent, webjarRef: AtomicReference[Option[Route]]): Unit = {
    webjarRef.get.foreach(route ⇒ routeManager.ref ! RouteRemoved(route))
  }

  override def modifiedBundle(bundle: Bundle, event: BundleEvent, webjarRef: AtomicReference[Option[Route]]): Unit = {
    val route = makeRoute(bundle)
    webjarRef.getAndSet(route).foreach(routeManager.ref ! RouteRemoved(_))
    route.foreach(routeManager.ref ! RouteAdded(_))
  }

  private def makeRoute(bundle: Bundle): Option[Route] = {
    Seq(resources(bundle), welcomeFiles(bundle)).flatten.reduceRightOption(_ ~ _)
  }

  private def resources(bundle: Bundle): Option[Route] = {
    Option(bundle.getEntry(basePath)).map { _ ⇒
      val baseURI = bundle.getEntry(basePath).toURI
      val URIs = bundle.findEntries(basePath, "*", true).map(_.toURI).toSeq
      val paths = URIs.map(baseURI.relativize(_).toString)
      routeManager.getBundleResources(bundle, paths, basePath)
    }
  }

  private def welcomeFiles(bundle: Bundle): Option[Route] = {
    Option(bundle.findEntries(basePath, welcomeFile, true)).flatMap { e ⇒
      val baseURI = bundle.getEntry(basePath).toURI
      e.map {
        u ⇒
          val p = baseURI.relativize(u.toURI).toString
          val dir = p.substring(0, math.max(p.lastIndexOf('/'), 0))
          path(dir) {
            routeManager.getBundleResource(bundle, s"$basePath/$p")
          }
      }.reduceRightOption(_ ~ _)
    }
  }
}