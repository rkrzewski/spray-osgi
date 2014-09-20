package io.spray.osgi.webjars

import java.util.concurrent.atomic.AtomicReference

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

  var rjsWebjars: Set[Webjar] = Set()

  val configRoute: AtomicReference[Option[Route]] = new AtomicReference(None)

  val shorthandRoute: AtomicReference[Option[Route]] = new AtomicReference(None)

  def receive = {
    case Added(w) =>
      w match {
        case Webjar("requirejs", _, _, bundle) =>
          updateRoute(Some(makeShorthandRoute(bundle)), shorthandRoute)
        case Webjar(_, _, Some(_), _) =>
          rjsWebjars += w
          updateRoute(makeConfigRoute(rjsWebjars), configRoute)
      }
    case Removed(w) =>
      w match {
        case Webjar("requirejs", _, _, _) =>
          updateRoute(None, shorthandRoute)
        case Webjar(_, _, Some(_), _) =>
          rjsWebjars -= w
          updateRoute(makeConfigRoute(rjsWebjars), configRoute)
      }
  }

  def makeShorthandRoute(bundle: Bundle): Route = {
    val urls = bundle.findEntries("/META-INF/resources", "*.js", true)
    urls.map { url =>
      val file = url.getPath.split("/").toSeq.reverse.head
      path("webjars" / file) {
        routeManager.getBundleResource(bundle, url.getPath)
      }
    }.reduceRight(_ ~ _)
  }

  def makeConfigRoute(webjars: Set[Webjar]): Option[Route] = {
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

  def updateRoute(newRoute: Option[Route], routeRef: AtomicReference[Option[Route]]) = {
	  routeRef.getAndSet(newRoute).foreach(routeManager.ref ! RouteRemoved(_))
    newRoute.foreach(routeManager.ref ! RouteAdded(_, config.ranking))
  }
}
