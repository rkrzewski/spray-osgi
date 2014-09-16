package io.spray.osgi.webjars

import org.osgi.framework.Version
import spray.routing.Route

case class Webjar(artifact: String, version: Version, resourcesRoute: Route)