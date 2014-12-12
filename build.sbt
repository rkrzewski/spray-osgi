import Dependencies._

lazy val basicSettings = Seq(
	scalaVersion := "2.11.4",
	scalaSource in Compile := baseDirectory.value / "src",
	scalaSource in Test := baseDirectory.value / "test"
)

lazy val configOsgi = (project in file("com.typesafe.config.osgi")).
	settings(basicSettings: _*).
	settings(libraryDependencies ++= 
		compileDep(typesafeConfig) ++ 
		compileDep(osgiCore) ++ 
		compileDep(osgiEnterprise))
	

lazy val akkaOsgiDs = (project in file("com.typesafe.akka.osgi.ds")).
	settings(basicSettings: _*).
	dependsOn(configOsgi).
	settings(libraryDependencies ++= 
		compileDep(akkaActor) ++
		compileDep(akkaOsgi))
	
lazy val sprayOsgi = (project in file("io.spray.osgi")).
	settings(basicSettings: _*).
	dependsOn(akkaOsgiDs).
	settings(libraryDependencies ++= 
		compileDep(sprayCan) ++
		compileDep(sprayRouting))
	
lazy val sprayOsgiWebjars = (project in file("io.spray.osgi.webjars")).
	settings(basicSettings: _*).
	dependsOn(sprayOsgi).
	settings(libraryDependencies ++= 
		compileDep(jacksonDatabind))

