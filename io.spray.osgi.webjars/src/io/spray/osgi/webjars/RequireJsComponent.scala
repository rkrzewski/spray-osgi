package io.spray.osgi.webjars

import scala.annotation.meta.setter
import scala.collection.JavaConversions.enumerationAsScalaIterator

import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference

import RequireJs.Added
import RequireJs.Removed
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.actorRef2Scala
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.osgi.RouteManager
import spray.osgi.RouteManager.RouteAdded
import spray.osgi.RouteManager.RouteRemoved
import spray.routing.Directive.pimpApply
import spray.routing.Directives.complete
import spray.routing.Directives.path
import spray.routing.Directives.respondWithMediaType
import spray.routing.PathMatcher.segmentStringToPathMatcher
import spray.routing.Route
import spray.routing.RouteConcatenation.pimpRouteWithConcatenation

@Component
class RequireJsComponent extends BaseComponent with RequireJs {

  @(Reference @setter)
  private var actorSystem: ActorSystem = _

  @(Reference @setter)
  private var routeManager: RouteManager = _

  private var requireJsActor: ActorRef = _

  private var config: Config = _

  @Activate
  def activate(ctx: BundleContext, properties: java.util.Map[String, _]): Unit = {
    config = Config(properties)
    requireJsActor = actorSystem.actorOf(Props(classOf[RequireJsActor], routeManager, config))
  }

  @Deactivate
  def deactivate: Unit = {
    requireJsActor ! PoisonPill
  }

  def ref = requireJsActor
}

class RequireJsActor(routeManager: RouteManager, config: BaseComponent#Config) extends Actor {
  val configActor = context.actorOf(Props(classOf[RequireJsConfigActor], routeManager, config))
  val shorthandActor = context.actorOf(Props(classOf[RequireJsShorthandActor], routeManager, config))
  def receive = {
    case msg =>
      configActor ! msg
      shorthandActor ! msg
  }
}

class RequireJsShorthandActor(routeManager: RouteManager, config: BaseComponent#Config) extends Actor {
  import RequireJs._
  import RouteManager._

  var route: Route = _

  def receive = {
    case Added(Webjar("requirejs", _, _, bundle)) =>
      route = makeRoute(bundle)
      routeManager.ref ! RouteAdded(route, config.ranking)
    case Removed(Webjar("requirejs", _, _, _)) =>
      routeManager.ref ! RouteRemoved(route)
  }

  def makeRoute(bundle: Bundle): Route = {
    val urls = bundle.findEntries("/META-INF/resources", "*.js", true)
    urls.map { url =>
      val file = url.getPath.split("/").toSeq.reverse.head
      path("webjars" / file) {
        routeManager.getBundleResource(bundle, url.getPath)
      }
    }.reduceRight(_ ~ _)
  }
}

class RequireJsConfigActor(routeManager: RouteManager, config: BaseComponent#Config) extends Actor {

  import RequireJs._
  import RouteManager._

  var webjars: Set[Webjar] = Set()

  var route: Option[Route] = None

  def receive = {
    case Added(w @ Webjar(_, _, Some(_), _)) =>
      webjars += w
      updateRoute
    case Removed(w @ Webjar(_, _, Some(_), _)) =>
      webjars -= w
      updateRoute
  }

  def updateRoute: Unit = {
    route.foreach(routeManager.ref ! RouteRemoved(_))
    route = makeRoute
    route.foreach(routeManager.ref ! RouteAdded(_, config.ranking))
  }

  def makeRoute: Option[Route] = {
    if (webjars.isEmpty) {
      None
    } else {
      val conf = webjars.toSeq.sortBy(_.artifact).flatMap(_.requireJsConfig)
      Some(path("webjars" / "requirejsConfig.js") {
        respondWithMediaType(`application/javascript`) {
          complete {
            s"""
              |var require = {
              |  callback : function() {
              |${conf.map(c => s"    requirejs.config($c);").mkString("\n")}
              |  }
              |};
            """.stripMargin
          }
        }
      })
    }
  }
}
