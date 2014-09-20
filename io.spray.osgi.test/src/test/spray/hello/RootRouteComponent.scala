package test.spray.hello

import scala.annotation.meta.setter

import org.osgi.framework.BundleContext
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference

import spray.osgi.RouteManager
import spray.routing.Route
import spray.routing.Directives.path

@Component
class RootRouteComponent {
  import RouteManager._

  @(Reference @setter)
  var routeManager: RouteManager = _

  var rootRoute: Route = _

  @Activate
  def activate(ctx: BundleContext): Unit = {
    rootRoute = path("") {
      routeManager.getBundleResource(ctx.getBundle, "static/index.html")
    }
    routeManager.ref ! RouteAdded(rootRoute)
  }

  @Deactivate
  def deactivate(): Unit = {
    routeManager.ref ! RouteRemoved(rootRoute)
  }
}