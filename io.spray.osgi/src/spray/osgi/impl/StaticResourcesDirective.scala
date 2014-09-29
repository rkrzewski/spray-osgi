package spray.osgi.impl

import org.osgi.framework.Bundle
import org.parboiled.common.FileUtils

import akka.actor.ActorRefFactory
import spray.http.ContentType
import spray.http.ContentTypes.NoContentType
import spray.http.DateTime
import spray.http.EntityTag
import spray.http.MediaTypes
import spray.httpx.marshalling.BasicMarshallers
import spray.httpx.marshalling.MultipartMarshallers.multipartByterangesMarshaller
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import spray.routing.Directive0
import spray.routing.Directives.autoChunk
import spray.routing.Directives.complete
import spray.routing.Directives.conditional
import spray.routing.Directives.detach
import spray.routing.Directives.get
import spray.routing.Directives.path
import spray.routing.Directives.pathPrefix
import spray.routing.Directives.reject
import spray.routing.Directives.withRangeSupport
import spray.routing.PathMatcher.segmentStringToPathMatcher
import spray.routing.Route
import spray.routing.RouteConcatenation.pimpRouteWithConcatenation
import spray.routing.RoutingSettings
import spray.routing.directives.BasicDirectives
import spray.routing.directives.ChunkSizeMagnet.fromThresholdAndChunkSize
import spray.routing.directives.DetachMagnet.fromUnit
import spray.routing.directives.RangeDirectives.WithRangeSupportMagnet.fromSettings

object StaticResourcesDirective {

  private def autoChunked(implicit settings: RoutingSettings, refFactory: ActorRefFactory): Directive0 =
    autoChunk((settings.fileChunkingThresholdSize, settings.fileChunkingChunkSize))

  private def conditionalFor(length: Long, lastModified: Long)(implicit settings: RoutingSettings): Directive0 =
    if (settings.fileGetConditional) {
      val tag = java.lang.Long.toHexString(lastModified ^ java.lang.Long.reverse(length))
      val lastModifiedDateTime = DateTime(math.min(lastModified, System.currentTimeMillis))
      conditional(EntityTag(tag), lastModifiedDateTime)
    } else BasicDirectives.noop

  private val Extension = ".*\\.(.*)$".r

  private def contentType(path: String): ContentType =
    path match {
      case Extension(ext) ⇒ MediaTypes.forExtension(ext) map (ContentType(_)) getOrElse (NoContentType)
      case _ ⇒ NoContentType
    }

  /**
   * Completes GET requests with the content of the given resource. The actual I/O operation is
   * running detached in a `Future`, so it doesn't block the current thread (but potentially
   * some other thread !).
   * If the file cannot be found or read the Route rejects the request.
   */
  def getBundleResource(bundle: Bundle, resourceName: String)(implicit settings: RoutingSettings, refFactory: ActorRefFactory): Route = {
    get {
      detach() {
        bundle.getEntry(resourceName) match {
          case null ⇒ reject
          case url ⇒
            val (length, lastModified) = {
              val conn = url.openConnection()
              try {
                conn.setUseCaches(false) // otherwise the JDK will keep the JAR file open when we close!
                val len = conn.getContentLength
                val lm = conn.getLastModified
                len → lm
              } finally { conn.getInputStream.close() }
            }
            implicit val bufferMarshaller = BasicMarshallers.byteArrayMarshaller(contentType(resourceName))
            autoChunked.apply { // TODO: add implicit RoutingSettings to method and use here
              conditionalFor(length, lastModified).apply {
                withRangeSupport() {
                  complete {
                    // readAllBytes closes the InputStream when done, which ends up closing the JAR file
                    // if caching has been disabled on the connection
                    val is = url.openStream()
                    try { FileUtils.readAllBytes(is) }
                    finally { is.close() }
                  }
                }
              }
            }
        }
      }
    }
  }

  def getBundleResources(bundle: Bundle, paths: Seq[String], resBasePath: String)(implicit settings: RoutingSettings, refFactory: ActorRefFactory): Route = {

    def buildRoute(pathSegments: Seq[Seq[String]], basePath: String): Route = {
      // group paths by leading segments
      val groupedPaths = pathSegments.groupBy(_.head).toSeq
      groupedPaths.map {
        // only one path starting with given segments
        case (_, Seq(p)) ⇒
          val uniquePath = p.mkString("/")
          path(uniquePath).apply {
            getBundleResource(bundle, resBasePath + basePath + "/" + uniquePath)
          }
        // multiple paths starting with given segments
        case (segment, paths) ⇒
          pathPrefix(segment).apply {
            // strip leading segment from paths
            val nestedPaths = paths.map(_.tail).filterNot(_.isEmpty)
            buildRoute(nestedPaths, basePath + "/" + segment)
          }
      }.reduceLeft(_ ~ _)
    }

    buildRoute(paths.map(_.split("/").toSeq), "")
  }
}