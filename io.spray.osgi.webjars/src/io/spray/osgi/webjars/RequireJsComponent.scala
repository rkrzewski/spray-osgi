package io.spray.osgi.webjars

import scala.annotation.meta.setter
import scala.collection.JavaConversions._
import org.osgi.framework.BundleContext
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.PoisonPill
import spray.osgi.RouteManager
import spray.osgi.BundleResourcesRouteService
import spray.routing.Route
import org.osgi.framework.Bundle
import spray.routing.Route
import spray.routing.Directive.pimpApply
import spray.routing.Directives.path
import spray.routing.PathMatcher.segmentStringToPathMatcher
import spray.routing.RouteConcatenation.pimpRouteWithConcatenation

@Component
class RequireJsComponent extends BaseComponent with RequireJs {

  @(Reference @setter)
  private var actorSystem: ActorSystem = _

  @(Reference @setter)
  private var routeManager: RouteManager = _

  @(Reference @setter)
  private var routeService: BundleResourcesRouteService = _

  private var requireJsActor: ActorRef = _

  private var config: Config = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    config = Config(properties)
    requireJsActor = actorSystem.actorOf(Props(classOf[RequireJsActor], routeManager(), routeService, config))
  }

  @Deactivate
  def deactivate: Unit = {
    requireJsActor ! PoisonPill
  }

  def ref = requireJsActor
}

class RequireJsActor(routeManager: ActorRef, routeService: BundleResourcesRouteService, config: BaseComponent#Config) extends Actor {
  val shorthand = context.actorOf(Props(classOf[RequireJsShorthandActor], routeManager, routeService, config))
  def receive = {
    case msg =>
      shorthand ! msg
  }
}

class RequireJsShorthandActor(routeManager: ActorRef, routeService: BundleResourcesRouteService, config: BaseComponent#Config) extends Actor {
  import RequireJs._
  import RouteManager._

  var route: Route = _

  def receive = {
    case Added(Webjar("requirejs", _, _, bundle)) =>
      route = makeRoute(bundle)
      routeManager ! RouteAdded(route, config.ranking)
    case Removed(Webjar("requirejs", _, _, _)) =>
      routeManager ! RouteRemoved(route)
  }

  def makeRoute(bundle: Bundle): Route = {
    val urls = bundle.findEntries("/META-INF/resources", "*.js", true)
    urls.map { url =>
      val file = url.getPath.split("/").toSeq.reverse.head
      path("webjars" / file) {
        routeService.getBundleResource(bundle, url.getPath)
      }
    }.reduceRight(_ ~ _)
  }
}
