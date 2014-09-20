package io.spray.osgi.webjars

import scala.annotation.meta.setter
import scala.collection.JavaConversions._
import org.osgi.framework.BundleContext
import org.osgi.framework.Version
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference
import spray.osgi.RouteManager
import spray.routing.Route
import org.osgi.framework.Bundle

@Component(configurationPid = "io.spray.webjars")
class WebjarsComponent extends BaseComponent {

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

  def register(webjar: Webjar): Unit = {
    requireJs.ref ! RequireJs.Added(webjar)
  }

  def unregister(webjar: Webjar): Unit = {
    requireJs.ref ! RequireJs.Removed(webjar)
  }

}