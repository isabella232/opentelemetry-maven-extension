# Maven OpenTelemetry extension

```
export OTEL_RESOURCE_ATTRIBUTES=service.name=maven,service.version=3.8.1
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"

mvn clean verify
```

![](https://github.com/cyrille-leclerc/maven-opentelemetry-extension/raw/main/docs/images/maven-execution-trace-jaeger.png)