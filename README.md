# Maven OpenTelemetry extension

## Getting Started

### Adding the extension to the classpath

```
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"

mvn -Dmaven.ext.class.path=path/to/opentelemetry-maven-extension.jar clean verify
```

### Declaring the extension `pom.xml`

Configuration MUST be passed as environment variables with: `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_EXPORTER_OTLP_HEADERS`, `OTEL_RESOURCE_ATTRIBUTES`, `OTEL_EXPORTER_OTLP_TIMEOUT`.


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


## Example

![](https://github.com/cyrille-leclerc/maven-opentelemetry-extension/raw/main/docs/images/maven-execution-trace-jaeger.png)



## Example of a distributed trace of a Jenkins pipeline executing a Maven build

Distributed trace combining the OpenTelemetry Maven Extension with the [Jenkins OpenTelemetry plugin](https://plugins.jenkins.io/opentelemetry/).

### Trace visualized with [Elastic Observability](https://www.elastic.co/observability)

![](https://raw.githubusercontent.com/cyrille-leclerc/opentelemetry-maven-extension/main/docs/images/jenkins-maven-execution-trace-elastic.png)

### Trace visualized with [Jaeger Tracing](https://www.jaegertracing.io/)

![](https://raw.githubusercontent.com/cyrille-leclerc/opentelemetry-maven-extension/main/docs/images/jenkins-maven-execution-trace-jaeger.png)
