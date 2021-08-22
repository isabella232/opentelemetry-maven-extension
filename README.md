# Maven OpenTelemetry extension

```
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"

mvn -Dmaven.ext.class.path=path/to/opentelemetry-maven-extension.jar clean verify
```

![](https://github.com/cyrille-leclerc/maven-opentelemetry-extension/raw/main/docs/images/maven-execution-trace-jaeger.png)



## Example of a distributed trace of a Jenkins pipeline executing a Maven build

Distributed trace combining the OpenTelemetry Maven Extension with the [Jenkins OpenTelemetry plugin](https://plugins.jenkins.io/opentelemetry/).

### Trace visualized with [Elastic Observability](https://www.elastic.co/observability)

![](https://raw.githubusercontent.com/cyrille-leclerc/opentelemetry-maven-extension/main/docs/images/jenkins-maven-execution-trace-elastic.png)

### Trace visualized with [Jaeger Tracing](https://www.jaegertracing.io/)

![](https://raw.githubusercontent.com/cyrille-leclerc/opentelemetry-maven-extension/main/docs/images/jenkins-maven-execution-trace-jaeger.png)
