export OTEL_RESOURCE_ATTRIBUTES=service.name=maven,service.version=3.8.1
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"


export OTEL_COLLECTOR_EXPORTER_OTLP_ENDPOINT="http://localhost:8200"
export OTEL_COLLECTOR_EXPORTER_OTLP_BEARER="my_secret_token"

