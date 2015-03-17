# Console log agent

This bundle provides a Declarative Services component that registers a `LogListener` with the
`LogReaderService` present in the framework, that prints log events to JVM's standard output,
as recommended in [12 factor application principles](http://12factor.net/logs)

## Requirements

In order to run this bundle, Declarative Services runtime (OSGi Enterprise ch. 112) and a provider 
of LogService (OSGi Enterprise ch. 101) must be installed, for example:
    
   *   [Apache Felix SCR](http://felix.apache.org/documentation/subprojects/apache-felix-service-component-runtime.html)
   *   [Apache Felix Log](http://felix.apache.org/documentation/subprojects/apache-felix-log.html)

## Configuration

Verbosity of logging output may be controlled using `console.log.level` framework property. 
Use the following values:

   *   4 - print all messages
   *   3 - print INFO, WARNING and ERROR messages
   *   2 - print WARNING and ERROR messages
   *   1 - print only ERROR messages
   
If you need to monitor initialization of your application, you should make sure that LogService in 
launched early using StartLevel API (OSGi Core R5 ch. 9). 
   
## Output format

Each log entry is printed to the console in a separate line of the following format:

<pre>
`ISO-8601 timestamp` `bundle symbolic name` [`bundle id`]: `message`
</pre>
 
If the log entry contains a `Throwable` the stack trace will be appended in the following lines.