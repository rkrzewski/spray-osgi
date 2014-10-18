# OSGi ConfigurationAdmin plugin for Typesafe Config library

## Backgroung information

`ConfigurationAdmin` (defined in OSGi Enterprise specification chapter 104) provides a generic 
mechanism for supplying configuration to _managed services_ deployed in OSGi framework. Managed 
services are identified with _Persistent Identity_ (PID), strings that conform to OSGi symbolic name
syntax. A configuration is dictionary of name to value mappings, where names are (also using 
symbolic name syntax) and values may be Strings and Java primitive types, or arrays thereof.

ConfigurationAdmin acts only as a mediator: it does not handle  injection of configuration into 
components nor persistent storage of configurations. Bundles interested in configurations for a 
specific PID register a `ManagedService` or `ManagedServiceFactory` services with the framework, and 
ConfigurationAdmin implementation informs them when matching configuration becomes available / 
unavailable or is modified. Similarly, components that that handle storage and manipulation of 
configurations, acquire reference to `ConfigurationAdmin` from the framework and invoke methods on
it to have the configurations propagated to clients.

OSGi Declarative Services (Enterprise spec ch. 112) allow specifying PIDs for components and
provide a number of choices regarding components lifecycle with regards to configuration. A 
component may be kept in _unsatisied_ state until suitable configuration becomes available, 
or may be stared with null configuration when none is defined. The component may be notified when 
configuration changes, or it may be disposed and a new instance may be spawned with the updated 
configuration.

[Apache Felix](http://felix.apache.org/) project provides implementations of 
[ConfigurationAdmin](http://felix.apache.org/documentation/subprojects/apache-felix-config-admin.html)
and [Declarative Services](http://felix.apache.org/documentation/subprojects/apache-felix-service-component-runtime.html)
and in addition to that, Apache Felix provides [File Install](http://felix.apache.org/documentation/subprojects/apache-felix-file-install.html)
a management agent that watches a disk directory for and installs any OSGi bundles present there into 
the framework, and if `ConfigurationAdmin` service is available, loads all Java properties files,
into it using `filename ::= <pid> ( '-' <subname> )? '.cfg'` convention.

[Typesafe Config](https://github.com/typesafehub/config) is a library for configuration management
for JVM languages, used by [Akka](http://akka.io/) and [Spray](http://spray.io/). It supports Java
properties format, JSON and also "Human-Optimized Config Object Notation" or 
[HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) which is a superset of JSON
with features added for making the files more pleasant to view and edit.

## What this bundle does

This bundle is a replacement for
[File Install](http://felix.apache.org/documentation/subprojects/apache-felix-file-install.html), 
reusing parts of it's implemenation (as noted in source files) with the following differences:

   *   Does not handle bundle installation
   *   Supports HOCON files with `<pid> ( '-' <subname> )? '.conf'` instead of Java properties
   *   Uses `java.nio.file.WatchService` (available in JDK7+) instead of polling

For each setting in the original configuration file, the following entries are created in the 
configuration dictionary passed to ConfigurationAdmin: If the value is a substitution (`${setting}`)
`<complete entry path>.expr` entry is created, otherwise `<complete entry path>` is created,
holding actual value (may be either a String, Integer, Long, Double, or Boolean). Additionally
`<complete entry path>.origin` String entry is created, holding origin description (file path and 
line number).

In order to reconstruct `Config` object inside the target `ManagedService` or component, 
`com.typesafe.config.osgi.ConfigRecovery` utility class is provided. Using it is preferable to 
decoding the configuration by hand, because it hides the details of encoding the configuration 
values in `ConfigurationAdmin` properties, and using it shields the application from possible 
issues should those details change in the future. 

The `Config` object recovered from `ConfigurationAdmin` properties needs to be combined with
`reference.conf` settings (`akka.osgi.BundleDelegatingClassLoader` implements the correct strategy
for accessing classpath resources in OSGi environment) and `resolve` needs to be invoked on the
combined preferences to execute the substitutions. Doing this is responsibility of the user.

## Caveats / further work

   *   Only one directory can be watched at a time, and it's location is currently hardcoded to 
       `./conf` where `.` is the current directory of JVM. This could be made configurable using
       JVM system properties. 
   *   Java properties and JSON files are currently not supported by the plugin.
   *   Writing configuration changes back to disk is not supported. Felix File Install has the 
       capability to write configurations changed at runtime 
       (using [Felix Web Console](http://felix.apache.org/documentation/subprojects/apache-felix-web-console.html) 
       for example) back to disk. Since the configuration passed by ConfigurationAdmin are plain 
       name / value pairs, writing them naively to HOCON file would clobber comments and grouping. 
       Typesafe Config provides an elaborate model of configuration objects so I am guessing it should be
       possible to trace property values passed by ConfigurationAdmin back to their location in an
       existing HOCON file and update it, preserving grouping and formatting. New properties could
       be added at the end of the most nested section of the file matching the property key prefix.
       I haven't explored this possibility though.
   *   On several occasions my Declarative Components failed to start, and examining their status
       showed that configurations were not available. Touching the configuration files on disk
       triggered loading the configuration and activating components. I haven't determined yet
       if this was caused by a bug in this plugin or race condition in activating ConfigAdmin, SCR 
       and this plugin.