package test.spray.hello

import spray.routing.Directives._
import spray.httpx.marshalling.BasicMarshallers._
import org.osgi.service.component.annotations.Component
import spray.routing.Route
import spray.osgi.RouteService

@Component
class HelloEndpoint extends RouteService {
  def apply(): Route =
    path("hello") {
      get {
        complete {
          "Hello world!"
        }
      }
    }
}