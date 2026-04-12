# Kubernetes Deployment Guide

This directory contains the Helmfile-based deployment for the microservices demo.

## Prerequisites

Install the following tools before deploying:
- `kubectl`
- `helm`
- `helmfile`
- `helm-diff` plugin for Helm

Example installation on Linux:
```bash
# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/

# helm
curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# helmfile
curl -L https://github.com/helmfile/helmfile/releases/latest/download/helmfile-linux-amd64.tar.gz -o helmfile.tar.gz
tar -zxvf helmfile.tar.gz
sudo mv helmfile /usr/local/bin/

# helm-diff plugin
helm plugin install https://github.com/databus23/helm-diff
```

Example installation on Windows (PowerShell):
```powershell
# kubectl
choco install kubernetes-cli

# helm
choco install kubernetes-helm

# helmfile
choco install helmfile

# helm-diff plugin
helm plugin install https://github.com/databus23/helm-diff
```

## External dependencies (required before first deploy)

### nacos-k8s

The Nacos Helm chart is sourced from the [nacos-k8s](https://github.com/nacos-group/nacos-k8s) project and is **not bundled** in this repository. You must clone it manually before running `helmfile sync`:

```bash
git clone https://github.com/nacos-group/nacos-k8s.git k8s/external/nacos-k8s
```

`helmfile.yaml` references the chart at `./external/nacos-k8s/helm`. The directory must be present or every `helmfile` command that touches the `nacos` release will fail.

## Namespace

The `microservices` namespace is defined in `templates/namespace.yaml`. Helmfile creates it automatically during `helmfile sync` via `createNamespace: true`. When running a bare `helm upgrade --install` command, apply the namespace manifest first or add `--create-namespace`.

All workloads are deployed into the `microservices` namespace.

## Component versions

The following third-party components are pinned to specific versions in `helmfile.yaml` and `values/`.

| Component                  | Helm Chart Version | App / Image Version                              |
|----------------------------|--------------------|--------------------------------------------------|
| kube-prometheus-stack      | 83.4.0             | prometheus-operator v0.90.1, Grafana 12.4.2      |
| ECK operator               | latest (elastic)   | —                                                |
| ECK stack                  | 0.18.2             | Elasticsearch + Kibana managed by ECK            |
| Loki                       | 9.2.0              | —                                                |
| Tempo                      | 2.0.0              | 2.10.1                                           |
| Jaeger                     | 4.6.0              | —                                                |
| OpenTelemetry Collector    | 0.150.0            | otel/opentelemetry-collector-contrib **0.149.0** |
| RocketMQ                   | 0.0.1              | 1.16.0                                           |
| ingress-nginx              | 4.15.1             | 1.15.1                                           |

> **Note:** The OTel Collector Helm chart version (`0.150.0`) and the collector application image version (`0.149.0`) are separate version numbers — the chart has its own release cadence independent of the application binary.

## Deployment flow

The full stack is defined in `helmfile.yaml` and includes:

- Middleware: MySQL, Nacos, RocketMQ, RocketMQ topic initialization
- Observability: ECK operator/stack, kube-prometheus-stack, Grafana dashboards, Loki, Tempo, Jaeger, Zipkin, OpenTelemetry Collector
- Applications: gateway, multiplication, gamification, logs, frontend
- Ingress: ingress-nginx controller and shared ingress rules

Note: the `grafana-dashboards` release creates the dashboard ConfigMaps that `kube-prometheus-stack`/Grafana consumes. Helmfile already keeps the releases in the right order.

### Grafana dashboards

Dashboards are loaded from `charts/grafana-dashboards-configmap/dashboards/` via the Grafana sidecar. The following dashboards are currently included:

| File                          | Dashboard name              | Datasource        |
|-------------------------------|-----------------------------|-------------------|
| `demo-dashboard.json`         | Demo (service RED metrics)  | Prometheus        |
| `spanmetrics-dashboard.json`  | Spanmetrics                 | Prometheus        |
| `opentelemetry-collector.json`| OpenTelemetry Collector     | Prometheus        |

> To add a dashboard, drop a JSON file into the `dashboards/` directory and re-run `helmfile sync -l name=grafana-dashboards`. To remove a dashboard, delete the JSON file and re-run — the Grafana sidecar will remove it automatically within 30 seconds.

## Environment Variables

### Elasticsearch Authentication

The Grafana datasource configuration includes an Elasticsearch datasource that uses the `elastic` environment variable for authentication:

```yaml
- name: Elastic
  type: elasticsearch
  basicAuth: true
  basicAuthUser: elastic
  secureJsonData:
    basicAuthPassword: ${elastic}
  url: https://elasticsearch-es-http:9200
```

The `elastic` environment variable is injected directly from the `elasticsearch-es-elastic-user` Kubernetes Secret using `envFromSecret: elasticsearch-es-elastic-user` in the Grafana values. This Secret is created automatically by the ECK Elasticsearch operator in the `microservices` namespace.

## Deploy everything

From the `k8s/` directory:

```bash
helmfile sync
```

## Deploy selected groups

The releases are labeled so you can deploy parts of the stack independently.

### Middleware
```bash
helmfile sync -l name=mysql
helmfile sync -l name=nacos
helmfile sync -l name=rocketmq
helmfile sync -l name=rocketmq-init-topics
```

### Observability
```bash
helmfile sync -l stack=observability
```

### Applications
```bash
helmfile sync -l name=gateway
helmfile sync -l name=multiplication
helmfile sync -l name=gamification
helmfile sync -l name=logs
helmfile sync -l name=frontend
```

### Ingress
```bash
helmfile sync -l name=ingress-nginx
helmfile sync -l name=ingress-rules
```

## Single-release debug commands

Use these when you want to publish just one component and watch the result closely.

### Helmfile release commands
```bash
# Deploy the MySQL chart used by the demo services.
helmfile sync -l name=mysql

# Deploy Nacos for service discovery and configuration.
helmfile sync -l name=nacos

# Deploy RocketMQ and its supporting topic bootstrap Job.
helmfile sync -l name=rocketmq
helmfile sync -l name=rocketmq-init-topics

# Deploy observability components.
helmfile sync -l name=eck-operator
helmfile sync -l name=eck-stack
helmfile sync -l name=kube-prometheus-stack
helmfile sync -l name=grafana-dashboards
helmfile sync -l name=loki
helmfile sync -l name=tempo
helmfile sync -l name=jaeger
helmfile sync -l name=zipkin
helmfile sync -l name=otel-collector

# Deploy the application workloads.
helmfile sync -l name=sentinel-dashboard
helmfile sync -l name=gateway
helmfile sync -l name=multiplication
helmfile sync -l name=gamification
helmfile sync -l name=logs
helmfile sync -l name=frontend

# Deploy ingress components.
helmfile sync -l name=ingress-nginx
helmfile sync -l name=ingress-rules
```

### Helm install commands
```bash
# Install the local MySQL chart directly with Helm.
helm upgrade --install mysql ./charts/microservice -n microservices --create-namespace -f values/mysql.yaml

# Install Nacos from the local embedded chart (requires nacos-k8s to be cloned first — see above).
helm upgrade --install nacos ./external/nacos-k8s/helm -n microservices --create-namespace -f values/nacos.yaml

# Install RocketMQ from the upstream OCI chart.
helm upgrade --install rocketmq oci://registry-1.docker.io/apache/rocketmq --version 0.0.1 -n microservices --create-namespace -f values/rocketmq.yaml

# Install the RocketMQ topic initializer job.
helm upgrade --install rocketmq-init-topics ./charts/microservice -n microservices --create-namespace -f values/rocketmq-init-topics.yaml

# Install Grafana dashboard ConfigMaps before Grafana starts.
helm upgrade --install grafana-dashboards ./charts/grafana-dashboards-configmap -n microservices --create-namespace

# Install kube-prometheus-stack for Prometheus and Grafana.
# Pin to specific version to match cached images (prometheus-operator v0.90.1, grafana 12.4.2).
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack -n microservices --create-namespace -f values/kube-prometheus-stack.yaml --version 83.4.0

# Install Jaeger for distributed tracing.
helm upgrade --install jaeger jaegertracing/jaeger -n microservices --create-namespace -f values/jaeger.yaml

# Install OpenTelemetry Collector for traces, metrics, and logs.
# Chart 0.150.0 deploys app image otel/opentelemetry-collector-contrib:0.149.0.
helm upgrade --install otel-collector open-telemetry/opentelemetry-collector --version 0.150.0 -n microservices --create-namespace -f values/otel-collector.yaml

# Install ingress-nginx into its own namespace.
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace -f values/ingress-nginx.yaml
helm upgrade --install ingress-rules ./charts/ingress-rules -n microservices --create-namespace -f values/ingress-rules.yaml

# Install the frontend application.
helm upgrade --install frontend ./charts/microservice -n microservices --create-namespace -f values/frontend.yaml

# Install the Java backend services.
helm upgrade --install gateway ./charts/microservice -n microservices --create-namespace -f values/gateway.yaml
helm upgrade --install multiplication ./charts/microservice -n microservices --create-namespace -f values/multiplication.yaml
helm upgrade --install gamification ./charts/microservice -n microservices --create-namespace -f values/gamification.yaml
helm upgrade --install logs ./charts/microservice -n microservices --create-namespace -f values/logs.yaml
```

If you only want a render check before applying, use `helmfile template -l name=<release-name>`.

## Ingress

The ingress rules are defined in `charts/ingress-rules` and rendered from `values/ingress-rules.yaml`.

Current paths include:
- `/` → frontend
- `/api/(.*)` → gateway (rewrite: true)
- `/grafana` → Grafana
- `/prometheus` → Prometheus
- `/jaeger` → Jaeger
- `/zipkin` → Zipkin
- `/kibana` → Kibana over HTTPS backend
- `/nacos/(.*)` → Nacos (rewrite: true)
- `/sentinel-dashboard/` → Sentinel dashboard (rewrite: true)

### Path suffix configuration

Due to ingress path configuration, some services require base path configuration to work correctly:

| Service    | Ingress Path          | Configuration Required                                        |
|------------|-----------------------|---------------------------------------------------------------|
| Grafana    | `/grafana`            | `root_url: "%(protocol)s://%(domain)s/grafana/"` in grafana.ini |
| Prometheus | `/prometheus`         | No special configuration needed                               |
| Jaeger     | `/jaeger`             | `base_path: /jaeger` in jaeger-query config                   |
| Zipkin     | `/zipkin`             | `ZIPKIN_UI_BASEPATH: /zipkin` env variable                    |
| Kibana     | `/kibana`             | No special configuration needed                               |
| Nacos      | `/nacos/(.*)`         | Uses ingress rewrite, no service config needed                |
| Sentinel   | `/sentinel-dashboard/`| Uses ingress rewrite, no service config needed                |
| Gateway    | `/api/(.*)`           | Uses ingress rewrite, no service config needed                |

Services with `rewrite: true` in ingress-rules handle path rewriting at the ingress level.

## Port-forward examples

```bash
kubectl port-forward -n microservices svc/frontend                          3000:80
kubectl port-forward -n microservices svc/gateway                           8080:8000
kubectl port-forward -n microservices svc/kube-prometheus-stack-grafana     3001:80
kubectl port-forward -n microservices svc/kube-prometheus-stack-prometheus  9090:9090
kubectl port-forward -n microservices svc/jaeger-all-in-one                 16686:16686
kubectl port-forward -n microservices svc/zipkin                            9411:9411
kubectl port-forward -n microservices svc/eck-stack-eck-kibana-kb-http      5601:5601
kubectl port-forward -n microservices svc/sentinel-dashboard                8858:8080
```

## External access

If you use ingress locally, configure `/etc/hosts` with your ingress IP and the hostnames used by `values/gateway.yaml` and `values/frontend.yaml`.

## Registry Mirrors (Image Pull Acceleration)

Kind cluster is configured with containerd registry mirrors to accelerate image pulls.

### How It Works

Containerd uses `plugins."io.containerd.grpc.v1.cri".registry.mirrors` to configure image accelerators. When pulling an image, it tries each mirror in order and automatically falls back to the next if one fails.

### Configured Mirrors

| Source     | Mirrors                                                        |
|------------|----------------------------------------------------------------|
| docker.io  | Aliyun, DaoCloud, NetEase, Baidu and other China-region mirrors |
| quay.io    | DaoCloud mirror                                                |
| gcr.io     | DaoCloud mirror                                                |
| ghcr.io    | DaoCloud mirror                                                |

### Configuration

Configure registry mirrors in `kind/kind-config.yaml` before creating the cluster:

Modify `containerdConfigPatches` in `kind/kind-config.yaml`, then create the cluster:

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: kind

containerdConfigPatches:
  - |-
    [plugins."io.containerd.grpc.v1.cri".registry]
      config_path = "/etc/containerd/certs.d"
```

Then create corresponding host.toml config files in `kind/containerd-certs.d/` directory.

### Loading Local Images

For images with `pullPolicy: Never` (e.g., MySQL), you need to manually build and load them into the kind cluster:

```bash
# Build the image
docker build -t example/mysql:8.0.31 docker/image/mysql/8/

# Load into kind cluster
kind load docker-image example/mysql:8.0.31 --name kind

# Same for other microservice images
docker build -t multiplication:0.0.1-SNAPSHOT multiplication/
kind load docker-image multiplication:0.0.1-SNAPSHOT --name kind
```

## Teardown

```bash
helmfile destroy
kubectl delete namespace microservices
```

---

## Observability: Distributed Tracing Drill-down

This section explains how distributed traces are collected, stored, and surfaced in Grafana — including how you can drill down from a high-level metric into an individual trace and then into the related logs.

### Architecture overview

```
Spring Boot services
        │  OTLP (gRPC :4317 / HTTP :4318)
        ▼
OpenTelemetry Collector
        │
        ├─► Tempo          (trace storage)          ─► Grafana Explore / TraceQL
        ├─► Jaeger         (trace UI)               ─► Grafana Jaeger datasource
        ├─► Zipkin         (trace UI)               ─► Grafana Zipkin datasource
        ├─► spanmetrics    (connector, in-process)  ─► Prometheus scrape → Grafana metrics
        ├─► Loki           (structured logs)        ─► Grafana Logs / Explore
        └─► Elasticsearch  (raw log documents)      ─► Kibana / Grafana Elastic datasource
```

### Step 1 — OpenTelemetry Collector (`values/otel-collector.yaml`)

The Collector is the single ingestion point. Every backend service sends OTLP data to it.

**Receivers**

| Receiver      | Port | Protocol      | What it accepts                                      |
|---------------|------|---------------|------------------------------------------------------|
| `otlp` gRPC   | 4317 | gRPC          | Traces, metrics, logs from all Spring Boot services  |
| `otlp` HTTP   | 4318 | HTTP/protobuf | Same, for services that prefer HTTP                  |

**Connectors**

| Connector     | Purpose                                                                                             |
|---------------|-----------------------------------------------------------------------------------------------------|
| `spanmetrics` | Converts span data into RED metrics (rate, error, duration) **inside** the Collector. No extra scrape target needed. |

**Exporters**

| Exporter           | Destination                               | What is sent                                          |
|--------------------|-------------------------------------------|-------------------------------------------------------|
| `otlp_http/tempo`  | `http://tempo:4318`                       | All traces → Tempo for long-term storage and TraceQL  |
| `otlp_grpc`        | `jaeger-all-in-one:4317`                  | All traces → Jaeger UI                                |
| `zipkin`           | `http://zipkin:9411/api/v2/spans`         | All traces → Zipkin UI                                |
| `prometheus`       | `:8889` (scraped by Prometheus)           | spanmetrics output + collector self-metrics           |
| `otlp_http/loki`   | `http://loki:3100/otlp`                   | Structured logs → Loki                                |
| `elasticsearch`    | `https://elasticsearch-es-http:9200`      | Raw trace + log documents → ES/Kibana                 |

**Pipelines**

```
traces  pipeline: otlp → batch → [tempo, jaeger, zipkin, elasticsearch, spanmetrics]
metrics pipeline: otlp + spanmetrics → batch → prometheus
logs    pipeline: otlp → [loki, elasticsearch]
```

**Ports exposed as Kubernetes Services**

| Port  | Use                                                      |
|-------|----------------------------------------------------------|
| 4317  | OTLP gRPC receiver                                       |
| 4318  | OTLP HTTP receiver                                       |
| 8889  | Prometheus metrics exporter (scraped by kube-prometheus-stack) |
| 8888  | Collector self-metrics (also scraped by Prometheus)      |
| 13133 | Health check endpoint                                    |

### Step 2 — Tempo (`values/tempo.yaml`)

Tempo stores traces and makes them queryable via TraceQL.

**Distributor** — accepts OTLP gRPC (`:4317`) and HTTP (`:4318`) from the Collector.

**Metrics generator** — generates three kinds of derived metrics from stored spans:

| Processor        | What it produces                                                              | Key metrics                                                                                      |
|------------------|-------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `service-graphs` | Service-to-service request graphs                                             | `traces_service_graph_request_total`, `traces_service_graph_request_duration_seconds_bucket`     |
| `span-metrics`   | Per-span RED metrics (complements the Collector's spanmetrics connector)      | `traces_span_metrics_calls_total`, `traces_span_metrics_duration_milliseconds_bucket`            |
| `local-blocks`   | Enables TraceQL metrics queries directly against stored trace blocks          | powers `{...} \| rate()`, `\| quantile_over_time()`, etc.                                       |

**Storage** — traces are persisted on a local PVC (`/var/tempo/traces`, 5 Gi).

**Query frontend** — exposed on `:3200`, handles search, TraceQL, and `trace_by_id` requests from Grafana.

### Step 3 — Tempo datasource in Grafana (`values/kube-prometheus-stack.yaml`)

The Tempo datasource is configured with several drill-down links so that clicking any span in Grafana instantly opens related data in other datasources:

```yaml
- name: Tempo
  uid: tempo
  type: tempo
  url: http://tempo:3200
  jsonData:
    nodeGraph:
      enabled: true          # renders service dependency graph alongside traces
    tracesToMetrics:
      datasourceUid: webstore-metrics   # Prometheus datasource
      tags:
        - key: service.name
          value: service_name
      queries:
        - name: Span rate
          query: 'sum(rate(traces_span_metrics_calls_total{$__tags}[5m]))'
        - name: Span latency
          query: 'histogram_quantile(0.95, sum(rate(traces_span_metrics_duration_milliseconds_bucket{$__tags}[5m])) by (le, service_name))'
    tracesToLogsV2:
      datasourceUid: loki              # Loki datasource
      tags:
        - key: service.name
          value: service_name
      filterByTraceID: true
    lokiSearch:
      datasourceUid: loki
    serviceMap:
      datasourceUid: webstore-metrics  # Prometheus datasource for service map
```

### Drill-down flow end to end

```
Grafana dashboard (Prometheus metric panel)
   └─► click data point
       └─► Explore view shows spanmetrics (traces_span_metrics_calls_total, _duration_milliseconds_*)
           └─► "Query traces" link → Tempo Explore
               └─► TraceQL search returns matching traces
                   └─► click a trace → Span detail view
                       ├─► "Related metrics" → Prometheus panel filtered to this service/operation
                       └─► "Related logs" → Loki log stream filtered by traceId
```

> **Note — Explore Traces vs spanmetrics dashboards:** The Grafana Explore Traces page (via `grafana-exploretraces-app`) queries **Tempo directly using TraceQL** and is unaffected by metric name changes. Only the Prometheus-backed panels (Spanmetrics dashboard, Demo dashboard, and the `tracesToMetrics` drill-down links in Tempo/Zipkin datasources) depend on the spanmetrics metric names.

### Key metrics produced by spanmetrics

These metrics are generated by the `spanmetrics` connector in the Collector (and echoed by Tempo's `span-metrics` processor). Prometheus scrapes them from the Collector's `:8889` endpoint.

The OTel Collector prometheus exporter is configured with `add_metric_suffixes: true` (in `values/otel-collector.yaml`), which appends Prometheus-standard suffixes to metric names. Counter metrics get `_total`; histogram unit suffixes (`_milliseconds`) are preserved as-is.

| Metric                                          | Type      | Description                                      |
|-------------------------------------------------|-----------|--------------------------------------------------|
| `traces_span_metrics_calls_total`               | Counter   | Total number of spans per service/operation/status |
| `traces_span_metrics_duration_milliseconds_bucket` | Histogram | Span duration distribution (latency percentiles) |
| `traces_span_metrics_duration_milliseconds_sum` | Histogram | Sum of span durations                            |
| `traces_span_metrics_duration_milliseconds_count`| Histogram | Total span count (same as calls)                 |

> **Note:** With `add_metric_suffixes: true`, only the suffixed form is exported — the bare names (`traces_span_metrics_calls`, `traces_span_metrics_duration_bucket`) do **not** exist alongside them. All dashboard queries and Grafana datasource `tracesToMetrics` configurations must use the suffixed names.

**What is affected by the metric name change (`add_metric_suffixes: true`):**

| Component                                          | Affected? | Detail                                                                          |
|----------------------------------------------------|-----------|---------------------------------------------------------------------------------|
| Spanmetrics dashboard (`spanmetrics-dashboard.json`)| ✅ Yes   | All PromQL queries updated to `_total` / `_milliseconds_*`                      |
| Demo dashboard (`demo-dashboard.json`)             | ✅ Yes    | All PromQL queries updated                                                      |
| Tempo datasource `tracesToMetrics` links           | ✅ Yes    | Queries in `kube-prometheus-stack.yaml` updated; datasource ConfigMap patched   |
| Zipkin datasource `tracesToMetrics` links          | ✅ Yes    | Same — updated in `kube-prometheus-stack.yaml`                                  |
| Grafana Explore Traces (TraceQL)                   | ❌ No     | Queries Tempo directly over TraceQL, not Prometheus — unaffected                |
| Tempo Service Map                                  | ❌ No     | Uses `traces_service_graph_*` metrics, not spanmetrics                          |
| Jaeger / Zipkin trace UIs                          | ❌ No     | Display raw trace data, not Prometheus metrics                                  |
| OTel Collector dashboard (`opentelemetry-collector.json`) | ❌ No | Uses `otelcol_process_uptime_seconds_total` etc. — already correct           |

Common labels on all spanmetrics:

| Label          | Example value    | Source                              |
|----------------|------------------|-------------------------------------|
| `service_name` | `multiplication` | OTLP `service.name` resource attribute |
| `span_name`    | `POST /attempts` | Span name                           |
| `span_kind`    | `SPAN_KIND_SERVER` | Span kind                         |
| `status_code`  | `STATUS_CODE_OK` | OpenTelemetry span status           |

### Grafana plugins used for drilldown

| Plugin                          | Purpose                                          |
|---------------------------------|--------------------------------------------------|
| `grafana-exploretraces-app`     | Dedicated Traces Explore UI, TraceQL editor      |
| `grafana-metricsdrilldown-app`  | Click a metric → auto-generate related queries   |
| `grafana-lokiexplore-app`       | Log exploration from trace context               |
| `grafana-pyroscope-app`         | Continuous profiling (optional, included for future use) |
