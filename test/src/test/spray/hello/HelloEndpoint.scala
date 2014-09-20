package test.spray.hello

import spray.routing.Directives._
import spray.httpx.marshalling.BasicMarshallers._
import org.osgi.service.component.annotations.Component
import spray.routing.Route
import spray.osgi.RouteProvider

@Component
class HelloEndpoint extends RouteProvider {
  def route: Route =
    path("hello") {
      get {
        complete {
          "Hello world!"
        }
      }
    }
}