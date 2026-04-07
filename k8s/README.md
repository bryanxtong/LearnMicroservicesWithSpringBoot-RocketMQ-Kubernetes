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

All workloads are deployed into the `microservices` namespace.

## Deployment flow

The full stack is defined in `helmfile.yaml` and includes:

- Middleware: MySQL, Nacos, RocketMQ, RocketMQ topic initialization
- Observability: ECK operator/stack, kube-prometheus-stack, Grafana dashboards, Loki, Tempo, Jaeger, Zipkin, OpenTelemetry Collector
- Applications: gateway, multiplication, gamification, logs, frontend
- Ingress: ingress-nginx controller and shared ingress rules

Note: the `grafana-dashboards` release creates the dashboard ConfigMaps that `kube-prometheus-stack`/Grafana consumes. Helmfile already keeps the releases in the right order.

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
- `/sentinel` → Sentinel dashboard

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
