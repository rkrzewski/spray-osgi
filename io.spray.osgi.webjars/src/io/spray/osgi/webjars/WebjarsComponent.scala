package io.spray.osgi.webjars

import java.util.concurrent.atomic.AtomicReference

import scala.annotation.meta.setter
import scala.collection.JavaConversions._

import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.BundleEvent
import org.osgi.framework.Version
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference
import org.osgi.util.tracker.BundleTracker

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.PoisonPill
import spray.osgi.RouteManager
import spray.routing.Route

@Component(configurationPid = "io.spray.webjars")
class WebjarsComponent extends BaseComponent {

  @(Reference @setter)
  private var actorSystem: ActorSystem = _

  @(Reference @setter)
  var routeManager: RouteManager = _

  var tracker: WebjarBundleTracker = _

  var config: Config = _

  var webjarsActor: ActorRef = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    config = Config(properties)
    webjarsActor = actorSystem.actorOf(Props(classOf[WebjarsActor], routeManager, config))
    tracker = new WebjarBundleTracker(ctx)
    tracker.open()
  }

  @Deactivate
  def deactivate: Unit = {
    tracker.close()
    webjarsActor ! PoisonPill
  }

  import WebjarsActor._

  def register(webjar: Webjar): Unit = {
    webjarsActor ! WebjarAdded(webjar)
  }

  def unregister(webjar: Webjar): Unit = {
    webjarsActor ! WebjarRemoved(webjar)
  }

  class WebjarBundleTracker(ctx: BundleContext)
    extends BundleTracker[AtomicReference[Option[Webjar]]](ctx, Bundle.ACTIVE, null) {

    override def addingBundle(bundle: Bundle, event: BundleEvent): AtomicReference[Option[Webjar]] = {
      new AtomicReference(Webjar.load(bundle).map { w =>
        register(w)
        w
      })
    }

    override def removedBundle(bundle: Bundle, event: BundleEvent, webjarRef: AtomicReference[Option[Webjar]]): Unit = {
      webjarRef.get.foreach(w => unregister(w))
    }

    override def modifiedBundle(bundle: Bundle, event: BundleEvent, webjarRef: AtomicReference[Option[Webjar]]): Unit = {
      val newWebjar = Webjar.load(bundle)
      webjarRef.getAndSet(newWebjar).foreach(unregister(_))
      newWebjar.foreach(register(_))
    }
  }
}