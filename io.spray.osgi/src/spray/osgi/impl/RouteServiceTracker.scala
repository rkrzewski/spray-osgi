package spray.osgi.impl

import scala.annotation.meta.setter

import org.osgi.framework.BundleContext
import org.osgi.framework.Constants
import org.osgi.framework.ServiceReference
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference
import org.osgi.util.tracker.ServiceTracker
import org.osgi.util.tracker.ServiceTrackerCustomizer
import akka.actor.ActorSystem
import akka.actor.ActorRef
import spray.routing.Route
import spray.osgi.RouteService

class RouteServiceTracker(ctx: BundleContext, routeManager: ActorRef)
  extends ServiceTracker[RouteService, Route](ctx, classOf[RouteService], null) {
  import spray.osgi.RouteManager._

  override def addingService(ref: ServiceReference[RouteService]): Route = {
    val route = ctx.getService(ref)()
    routeManager ! RouteAdded(route, ranking(ref))
    route
  }

  override def modifiedService(ref: ServiceReference[RouteService], route: Route): Unit = {
    routeManager ! RouteModified(route, ranking(ref))
  }

  override def removedService(ref: ServiceReference[RouteService], route: Route): Unit = {
    ctx.ungetService(ref)
    routeManager ! RouteRemoved(route)
  }

  private def ranking(s: ServiceReference[_]): Int =
    s.getProperty(Constants.SERVICE_RANKING) match {
      case r: Integer => r
      case _ => 0
    }
}