package io.spray.osgi.webjars

import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference

import org.osgi.framework.BundleContext
import org.osgi.util.tracker.BundleTracker
import org.osgi.framework.Bundle
import org.osgi.framework.BundleEvent
import spray.osgi.RouteManager
import spray.routing.Route


class WebjarBundleTracker(ctx: BundleContext, manager: WebjarsComponent)
  extends BundleTracker[AtomicReference[Option[Webjar]]](ctx, Bundle.ACTIVE, null) {

  override def addingBundle(bundle: Bundle, event: BundleEvent): AtomicReference[Option[Webjar]] = {
    new AtomicReference(Webjar.load(bundle).map { w =>
      manager.register(w)
      w
    })
  }

  override def removedBundle(bundle: Bundle, event: BundleEvent, webjar: AtomicReference[Option[Webjar]]): Unit = {
    webjar.get.foreach(w => manager.unregister(w))
  }

  override def modifiedBundle(bundle: Bundle, event: BundleEvent, webjar: AtomicReference[Option[Webjar]]): Unit = {
    Webjar.load(bundle).flatMap { w =>
      manager.register(w)
      webjar.getAndSet(Some(w))
    }.orElse {
      webjar.getAndSet(None)
    }.foreach { w =>
      manager.unregister(w)
    }
  }
}