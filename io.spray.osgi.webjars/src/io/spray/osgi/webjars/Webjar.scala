package io.spray.osgi.webjars

import java.io.StringWriter

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.enumerationAsScalaIterator

import org.osgi.framework.Bundle
import org.w3c.dom.Document
import org.xml.sax.InputSource

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode

import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

case class Webjar(artifact: String, version: String, requireJsConfig: Option[String], bundle: Bundle)

object Webjar {

  def load(bundle: Bundle): Option[Webjar] = {
    readPom(bundle).map { pom =>
      val x = xpathFactory.newXPath()
      x.setNamespaceContext(nsContext)
      val artifact = x.evaluate("/project/artifactId/text()", pom)
      val projectVersion = x.evaluate("/project/version/text()", pom)
      val parentVersion = x.evaluate("/project/parent/version/text()", pom)
      val version = if (projectVersion == "") parentVersion else projectVersion
      Webjar(artifact, version, requireJsConfig(artifact, version, pom), bundle)
    }
  }

  private val docBuilderFactory = DocumentBuilderFactory.newInstance()

  private val xpathFactory = XPathFactory.newInstance()

  private val mapper = new ObjectMapper

  private val nsContext = new NamespaceContext {
    def getNamespaceURI(prefix: String): String = "http://maven.apache.org/POM/4.0.0"
    def getPrefix(nsUri: String): String = ???
    def getPrefixes(nsUri: String): java.util.Iterator[_] = ???
  }

  private def readPom(bundle: Bundle): Option[Document] = {
    val entriesOpt = Option(bundle.findEntries("META-INF/maven/org.webjars", "pom.xml", true))
    entriesOpt.flatMap { entries =>
      entries.toStream.headOption
    }.map { url =>
      val conn = url.openConnection();
      conn.setUseCaches(false)
      try {
        val source = new InputSource(conn.getInputStream)
        docBuilderFactory.newDocumentBuilder().parse(source)
      } finally {
        conn.getInputStream().close()
      }
    }
  }

  private def requireJsConfig(artifact: String, version: String, pom: Document): Option[String] = {

    def readConfig: Option[String] = {
      val x = xpathFactory.newXPath()
      x.setNamespaceContext(nsContext)
      Option(x.evaluate("//project/properties/requirejs/text()", pom))
    }

    def adjustPaths(rawConfig: String): String = {
      val tree = mapper.reader().readTree(rawConfig)
      val pathsNode = tree.findPath("paths")
      val basePath = s"/webjars/$artifact/$version/"
      if (pathsNode.getNodeType == JsonNodeType.OBJECT) {
        val paths = pathsNode.asInstanceOf[ObjectNode]
        paths.fieldNames.foreach { field => paths.put(field, basePath + paths.get(field).asText()) }
      }
      val sw = new StringWriter
      mapper.writer().writeValue(sw, tree)
      sw.toString
    }

    readConfig.map(adjustPaths(_)).filterNot(_ == "{}")
  }
}