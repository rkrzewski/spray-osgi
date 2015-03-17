import sbt._

object Dependencies {

	val akkaVersion	  = "2.3.9"
	val sprayVersion  = "1.3.2"
	val osgiVersion   = "5.0.0"
	
	val typesafeConfig  = "com.typesafe"               %  "config"                   % "1.2.1"
	val osgiCore        = "org.osgi"                   %  "org.osgi.core"            % osgiVersion
	val osgiEnterprise  = "org.osgi"                   %  "org.osgi.enterprise"      % osgiVersion
	val akkaActor       = "com.typesafe.akka"          %% "akka-actor"               % akkaVersion
	val akkaOsgi	    = "com.typesafe.akka"          %% "akka-osgi"                % akkaVersion
	val sprayCan        = "io.spray"                   %% "spray-can"                % sprayVersion
	val sprayRouting    = "io.spray"                   %% "spray-routing-shapeless2" % sprayVersion
	val jacksonDatabind = "com.fasterxml.jackson.core" %  "jackson-databind"         % "2.4.2"
	
	def compileDep   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
	def testDep      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")

}

