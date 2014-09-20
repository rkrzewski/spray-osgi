package spray.osgi.impl

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import spray.osgi.RouteManager
import spray.routing.Route
import spray.routing.HttpServiceActor
import spray.routing.RouteConcatenation._

class RouteManagerActor extends HttpServiceActor {
  import RouteManager._

  def watchRoutes(routes: Set[Route]): Receive = {
    case RouteAdded(route) =>
      useRoutes(routes + route) 
    case RouteRemoved(route) =>
      useRoutes(routes - route)
  }

  def useRoutes(routes: Set[Route]): Unit =
    context become (routes reduceRightOption (_ ~ _) match {
      case Some(route) =>
        watchRoutes(routes) orElse runRoute(route)
      case _ =>
        watchRoutes(routes)
    })

  def receive = watchRoutes(Set())
}
