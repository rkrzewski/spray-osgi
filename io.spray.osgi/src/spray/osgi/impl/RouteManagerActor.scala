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

  case class RouteWithRanking(route: Route, ranking: Int)

  implicit object RouteOrdering extends Ordering[RouteWithRanking] {
    def compare(a: RouteWithRanking, b: RouteWithRanking) =
      a.ranking - b.ranking
  }

  def watchRoutes(routes: Seq[RouteWithRanking]): Receive = {
    case RouteAdded(route, ranking) =>
      useRoutes((RouteWithRanking(route, ranking) +: routes).sorted) 
    case RouteModified(route, ranking) =>
      useRoutes((RouteWithRanking(route, ranking) +: routes.filterNot(_.route eq route)).sorted)
    case RouteRemoved(route) =>
      useRoutes(routes.filterNot(_.route eq route))
  }

  def useRoutes(routes: Seq[RouteWithRanking]): Unit =
    context become (routes map (_.route) reduceRightOption (_ ~ _) match {
      case Some(route) =>
        watchRoutes(routes) orElse runRoute(route)
      case _ =>
        watchRoutes(routes)
    })

  def receive = watchRoutes(Seq())
}
