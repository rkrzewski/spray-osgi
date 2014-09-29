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

object WebjarsActor {
  case class WebjarAdded(webjar: Webjar)
  case class WebjarRemoved(webjar: Webjar)
}

class WebjarsActor(routeManager: RouteManager) extends Actor {
  import WebjarsActor._

  var rjsWebjars: Set[Webjar] = Set()

  var resourceRoutes: Map[Bundle, Route] = Map()

  val configRoute: AtomicReference[Option[Route]] = new AtomicReference(None)

  val shorthandRoute: AtomicReference[Option[Route]] = new AtomicReference(None)

  def receive = {
    case WebjarAdded(w) ⇒
      val r = makeResourcesRoute(w.bundle)
      resourceRoutes += w.bundle → r
      routeManager.ref ! RouteAdded(r)

      w match {
        case Webjar("requirejs", _, _, bundle) ⇒
          updateRoute(Some(makeShorthandRoute(bundle)), shorthandRoute)
        case Webjar(_, _, Some(_), _) ⇒
          rjsWebjars += w
          updateRoute(makeConfigRoute(rjsWebjars), configRoute)
      }

    case WebjarRemoved(w) ⇒
      resourceRoutes.get(w.bundle).foreach(r ⇒ routeManager.ref ! RouteRemoved(r))
      resourceRoutes -= w.bundle

      w match {
        case Webjar("requirejs", _, _, _) ⇒
          updateRoute(None, shorthandRoute)
        case Webjar(_, _, Some(_), _) ⇒
          rjsWebjars -= w
          updateRoute(makeConfigRoute(rjsWebjars), configRoute)
      }
  }

  def makeResourcesRoute(bundle: Bundle): Route = {
    val basePath = "META-INF/resources"
    val baseURI = bundle.getEntry(basePath).toURI
    val URIs = bundle.findEntries(basePath, "*", true).map(_.toURI).toSeq
    val paths = URIs.map(baseURI.relativize(_).toString)
    routeManager.getBundleResources(bundle, paths, basePath)
  }

  def makeShorthandRoute(bundle: Bundle): Route = {
    val urls = bundle.findEntries("/META-INF/resources", "*.js", true)
    urls.map { url ⇒
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
              |${conf.map(c ⇒ s"    requirejs.config($c);").mkString("\n")}
              |  }
              |};
            """.stripMargin.trim
          }
        }
      })
    }
  }

  def updateRoute(newRoute: Option[Route], routeRef: AtomicReference[Option[Route]]) = {
    routeRef.getAndSet(newRoute).foreach(routeManager.ref ! RouteRemoved(_))
    newRoute.foreach(routeManager.ref ! RouteAdded(_))
  }
}
