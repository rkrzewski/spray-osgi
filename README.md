# OSGi runtime platform for web applications using Scala, Akka and Spray

## Motivation

I like Akka and Spray a lot, and I'm looking forward to upcoming Akka-HTTP. I also happen to like 
OSGi and I think it is much underappreciated and misunderstood in JVM languages community. I find 
there is a good alignment between Akka's "Let it crash" principle and OSGi dynamic nature, and 
larger Akka applications could benefit benefit from OSGi modularisation mechanism. I wanted to
combine the two tools together, which proved to be quite challenging. There results however are
promising enough IMO to share the project with a wider audience. 

## Tools / Building / Running

At this point, Eclipse-based [Scala IDE](http://scala-ide.org/) together with 
[Bndtools](http://bndtools.org/) are necessary to develop / play with the platform. 

Currently, I am using [Luna SR1](http://eclipse.org/downloads/packages/eclipse-ide-java-developers/lunasr1). 
[Scala IDE 4.0.0M3 for Scala 2.11](http://scala-ide.org/download/milestone.html) and bleeding edge
Bndtools available at 
[Cloudbees](https://bndtools.ci.cloudbees.com/job/bndtools.master/lastSuccessfulBuild/artifact/build/generated/p2/)
In case you'd prefer to stay with released versions, you can try 
[Bndtools 2.4M1](http://bndtools.org/installation.html#2_4_m1). I'm also running JDK8 both to 
launch Eclipse and to build and launch Scala code. 

Right now it's not possible to build the project outside of Eclipse. I will work on sbt build later 
on.

To run the example application, you should crate an `OSGi Framework` launcher (in case you see 
two entries with this name make sure you use the one provided by Bndtools, not PDT) and choose
`io.spray.osgi.test/run.bndrun` as the run descriptor. The default settings should work out of the 
box. The launched application contains [Felix gogo](http://felix.apache.org/site/apache-felix-gogo.html) 
shell that you can use to inspect the bundles, services and components running in the framework. 

## Modules

### com.typesafe.akka.osgi.ds

provides `ActorSystemServiceFactory` which enables sharing a 
a single `ActorSystem` by multiple bundles deployed in the framework, and a Declarative 
Services component for booting an `ActorSystem` configured through OSGi `ConfigurationAdmin`.

### com.typesafe.config.osgi

[Apache Felix File Install](http://felix.apache.org/site/apache-felix-file-install.html) 
replacement using [Typesafe Config](https://github.com/typesafehub/config) HOCON files.

### io.spray.httpx

[spray-httpx](http://spray.io/documentation/1.2.2/spray-httpx/) bundle 
re-wrapped to correct some manifest headers.

### io.spray.osgi

provides a Declarative Services component configured through OSGi 
`ConfigurationAdmin` that launches [spray-can](http://spray.io/documentation/1.2.2/spray-can/) 
and publishes a service allowing components in the framework to expose Spray `Route`s.

### io.spray.osgi.test

A test/demonstration application.

### io.spray.osgi.webjars

provides runtime support for [webjars](http://www.webjars.org/)
on par with [Play Framework](https://www.playframework.com/) - webjar bundles deployed in
the framework have their static content served, and appropriate RequireJs configuration is 
generated.

### org.webjars.angular-ui-bootstrap

OSGi bundle-wrapped webjar for 
[Native AngularJS directives for Bootstrap](https://github.com/angular-ui/bootstrap)

### org.webjars.angular-ui-sortable

OSGi bundle-wrapped webjar for 
[AngularJS bindings for jQuery UI Sortable](https://github.com/angular-ui/ui-sortable)

### org.webjars.angularjs

OSGi bundle-wrapped webjar for [AngularJS](https://angularjs.org/) 

### org.webjars.bootstrap

OSGi bundle-wrapped webjar for [Twitter Bootstrap](http://getbootstrap.com/)

### org.webjars.jquery

OSGi bundle-wrapped webjar for [jQuery](http://jquery.com/)

### org.webjars.jquery-ui

OSGi bundle-wrapped webjar for [jQuery-UI](http://jqueryui.com/)

### org.webjars.requirejs

OSGi bundle-wrapped webjar for [RequireJS](http://requirejs.org/)

## Fun things to try

   * Change contents of `html` file in `io.spray.osgi.test`, save, observe "routes modified" 
     message in the console, refresh the page in browser
   * Change contents of Spray `Route` in `io.spray.osgi.test`, save, observe "routes modified" 
     message in the console, refresh the page in browser
   * Change HTTP port defined in `io.spray.osgi.test/conf/io.spra.can.conf`, save observe message
     in the console as the server shuts down and restarts with modified configuration
   * Open `http://localhost:8080/webjars/requirejsConfig.js` in the browser. Open 
     `io.spray.osgi.test/run.bndrun` and remove `org.webjars.angular-ui-sortable` from **Run 
     Requirements** section. Hit **Resolve** and then save the file. This causes a number of
     webjar bundles to be unloaded from the framework. Observe "routes modified" 
     messages in the console. Refresh `requirejsConfig.js` in the browser and notice that RequireJS
     settings for the unloaded webjars are gone.
   * Open `io.spray.osgi.test/run.bndrun` and press **Export** button in the top right corner
     of the wizard. Choose 'Executable JAR', 'Export to folder' and select a folder on your disk.
     Then copy `io.spray.osgi.test/conf` folder to the same folder where you have exported the
     application. Now, you should be able to run the application from command line using 
     `java -jar`.     

## Further work

### Documentation & tests

Obvioulsy, documentation is needed to make this work usable to people other than myself :) Tests
are also needed to improve the chance of accepting changes upstream. 

### Submitting relevant parts upstream

`ActorSystemServiceFactory` is a good candidate for upstream adoption. I'm not sure about 
Declarative Services components. It depend if Spray users will find this approach interesting.
Typesafe Config management agent also could potentially by interesting to other OSGi users. We'll 
see what the Config library maintainers think. 

### Generating `Provide-Capability` and `Require-Capability` headers

Right now, webjar bundles have `Provide-Capability` in `org.webjars` headers generated 
using `bnd` macros, and `Require-Capability` headers are added by hand. It is possible to write a
`bnd` plugin that could generate such headers from Maven POM files contained in upstream webjar
artifacts.

Another possibility is defining `org.requirejs` namespace and generating capability headers for 
webjar modules based on contained `js` files and requirement headers based on dependency information
contained in RequireJS shim configuration stored in POM files. It is also possible to generate 
requirement headers for non-webjar modules by analyzing the contents of contained `js` files and
detecting RequireJS `define` and `require` function invocations.

### Isolating actor trees created by different bundles

Right now the ActorSystem facade creates actors directly underneath the default guardian actor.
I think it would be better to create an intermediate actor with a randomized name for each bundle
that would perform the role of the guardian actor for each individual bundle. This would eliminate
the problem of name clashes if bundles decide to register top level actors with the same names. It
would also make actors created by a bundle private to itself, unless the bundle publishes ActorRef
for the actor through an OSGi service or other mechanism. That would provide better alignment with 
OSGi principle of bundle encapsulation.

### bnd / sbt integration

[sbt-osgi](https://github.com/sbt/sbt-osgi) plugin provides basic integration of `bnd` inside `sbt`
build process. It is possible to create an sbt configuration that will allow building all of the 
bundles in this repository. 

However, I don't think it's possible to use bnd repositories as the source for compilation 
classpath, which is necessary to achieve fidelity between IDE and CI builds.

There are important pieces of functionality in bnd that hopefully can be made available in sbt:
baselining (MiMa for OSGi) and exporting of executable JARs. The latter is especially important for
creating a CI / CD pipeline for projects using this runtime platform.

### Logging

The modules are not yet using OSGi Logging service, so Akka logging goes straight to standard output,
and there might be some random `println`s buried in the code as well as places that could use some
diagnostic logging but have none ATM.

### Akka HTTP

I'm planning to port the relevant parts to Akka HTTP once it is released.

### Akka remoting

Deserializing incoming messages requires appropriate classes being available on the classpath. When
`akka-remote` is deployed as a bundle, the application classes are not present on it's classpath.
OSGi `DynamicImport-Package: *` offers a potential solution, but it comes with it's own quirks. It 
would also (theoretically) allow instantiating arbitrary objects of any class present in any package
exported in the framework with a crafted remote message. In my opinion, client bundles should mark
the exported packages that contain message classes with an attribute and an extender component 
should track those exports and build a robust and safe classloader for message classes.
 
### Akka cluster

Cluster membership management is a system wide concern, that could be well handled by Declareative
Services / Configuration Admin managed component, reusable across applications.

### Akka persistence

I haven't looked much into Akka persistence yet, but definitely plan to do so. Perhaps there are
system wide concerns that could be generalized into a reusable component. Setting up event / 
snapshot storage connectivity seems a good candidate. 