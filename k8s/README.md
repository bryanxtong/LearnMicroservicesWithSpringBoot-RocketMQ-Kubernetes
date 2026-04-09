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

## Namespace

The `microservices` namespace is defined in `templates/namespace.yaml` and Helmfile can create it automatically during `helmfile sync`.

When running a single `helm upgrade --install` command, make sure the namespace already exists or add `--create-namespace` for the first install.

All workloads are deployed into the `microservices` namespace.

## Deployment flow

The full stack is defined in `helmfile.yaml` and includes:

- Middleware: MySQL, Nacos, RocketMQ, RocketMQ topic initialization
- Observability: ECK operator/stack, kube-prometheus-stack, Grafana dashboards, Loki, Tempo, Jaeger, Zipkin, OpenTelemetry Collector
- Applications: gateway, multiplication, gamification, logs, frontend
- Ingress: ingress-nginx controller and shared ingress rules

Note: the `grafana-dashboards` release creates the dashboard ConfigMaps that `kube-prometheus-stack`/Grafana consumes. Helmfile already keeps the releases in the right order.

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

# Install Nacos from the local embedded chart.
helm upgrade --install nacos ./external/nacos-k8s/helm -n microservices --create-namespace -f values/nacos.yaml

# Install RocketMQ from the upstream OCI chart.
helm upgrade --install rocketmq oci://registry-1.docker.io/apache/rocketmq --version 0.0.1 -n microservices --create-namespace -f values/rocketmq.yaml

# Install the RocketMQ topic initializer job.
helm upgrade --install rocketmq-init-topics ./charts/microservice -n microservices --create-namespace -f values/rocketmq-init-topics.yaml

# Install Grafana dashboard ConfigMaps before Grafana starts.
helm upgrade --install grafana-dashboards ./charts/grafana-dashboards-configmap -n microservices --create-namespace

# Install kube-prometheus-stack for Prometheus and Grafana.
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack -n microservices --create-namespace -f values/kube-prometheus-stack.yaml

# Install Jaeger for distributed tracing.
helm upgrade --install jaeger jaegertracing/jaeger -n microservices --create-namespace -f values/jaeger.yaml

# Install OpenTelemetry Collector for traces, metrics, and logs.
helm upgrade --install otel-collector open-telemetry/opentelemetry-collector -n microservices --create-namespace -f values/otel-collector.yaml

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
- `/api/(.*)` → gateway
- `/grafana` → Grafana
- `/prometheus` → Prometheus
- `/jaeger` → Jaeger
- `/zipkin` → Zipkin
- `/kibana` → Kibana over HTTPS backend
- `/nacos/(.*)` → Nacos
- `/sentinel-dashboard/` → Sentinel dashboard

## Port-forward examples

```bash
kubectl port-forward -n microservices svc/frontend 3000:80
kubectl port-forward -n microservices svc/gateway 8080:8000
kubectl port-forward -n microservices svc/kube-prometheus-stack-grafana 3001:80
kubectl port-forward -n microservices svc/kube-prometheus-stack-prometheus 9090:9090
kubectl port-forward -n microservices svc/jaeger-all-in-one 16686:16686
kubectl port-forward -n microservices svc/zipkin 9411:9411
kubectl port-forward -n microservices svc/eck-stack-eck-kibana-kb-http 5601:5601
kubectl port-forward -n microservices svc/sentinel-dashboard 8858:8080
```

## External access

If you use ingress locally, configure `/etc/hosts` with your ingress IP and the hostnames used by `values/gateway.yaml` and `values/frontend.yaml`.

## Teardown

```bash
helmfile destroy
kubectl delete namespace microservices
```
