# Maven OpenTelemetry extension

## Getting Started

The Maven OpenTelemetry Extension is configured using environment variables or JVM system properties and it can be added to a build using one of the following ways:
* adding the extension jar to `${maven.home}/lib/ext`
* adding the path to the extension jar to`-Dmaven.ext.class.path`,
* adding the extension as a build extension in the `pom.xml`,
* (since Maven 3.3.1) configuring the extension in `.mvn/extensions.xml`.


### Adding the extension to the classpath

Add the Maven OpenTelemetry Extension to `${maven.home}/lib/ext` or to the classpath using `-Dmaven.ext.class.path=`.

```
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"

mvn -Dmaven.ext.class.path=path/to/opentelemetry-maven-extension.jar clean verify
```

### Declaring the extension in the `pom.xml` file

Add the Maven OpenTelemetry Extension in the `pom.xml` file.

```xml
<project>
  ...
  <build>
    <extensions>
      <extension>
        <groupId>co.elastic.maven</groupId>
        <artifactId>opentelemetry-maven-extension</artifactId>
        <version>0.1.0-SNAPSHOT</version>
      </extension>
    </extensions>
  </build>
</project>
```

```
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"

mvn clean verify
```

### Using the extension as a plugin in `pom.xml`

Allows to configure the OpenTelemetry exporter in `pom.xml`.

⚠️ NOT implemented.

## Configuration

The Maven OpenTelemetry Extension supports a subset of the [OpenTelemetry auto configuration environment variables and JVM system properties](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure).

| System property              | Environment variable        | Description                                                               |
|------------------------------|-----------------------------|---------------------------------------------------------------------------|
| otel.exporter.otlp.endpoint  | OTEL_EXPORTER_OTLP_ENDPOINT | The OTLP traces and metrics endpoint to connect to. Must be a URL with a scheme of either `http` or `https` based on the use of TLS. Example `http://localhost:4317`.            |
| otel.exporter.otlp.headers   | OTEL_EXPORTER_OTLP_HEADERS  | Key-value pairs separated by commas to pass as request headers on OTLP trace and metrics requests.        |
| otel.exporter.otlp.timeout   | OTEL_EXPORTER_OTLP_TIMEOUT  | The maximum waiting time, in milliseconds, allowed to send each OTLP trace and metric batch. Default is `10000`.  |
| otel.resource.attributes | OTEL_RESOURCE_ATTRIBUTES | Specify resource attributes in the following format: key1=val1,key2=val2,key3=val3 |


ℹ️ The `service.name` is set by default to `maven`, it can be overwritten specifying resource atributes.


## Examples

![](https://github.com/cyrille-leclerc/maven-opentelemetry-extension/raw/main/docs/images/maven-execution-trace-jaeger.png)



## Example of a distributed trace of a Jenkins pipeline executing a Maven build

Distributed trace combining the OpenTelemetry Maven Extension with the [Jenkins OpenTelemetry plugin](https://plugins.jenkins.io/opentelemetry/).

### Trace visualized with [Elastic Observability](https://www.elastic.co/observability)

![](https://raw.githubusercontent.com/cyrille-leclerc/opentelemetry-maven-extension/main/docs/images/jenkins-maven-execution-trace-elastic.png)

### Trace visualized with [Jaeger Tracing](https://www.jaegertracing.io/)

![](https://raw.githubusercontent.com/cyrille-leclerc/opentelemetry-maven-extension/main/docs/images/jenkins-maven-execution-trace-jaeger.png)
