package test.spray.hello

import org.osgi.service.component.annotations.Component

import spray.httpx.marshalling.BasicMarshallers._
import spray.osgi.RouteProvider
import spray.routing.Directives._
import spray.routing.Route

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