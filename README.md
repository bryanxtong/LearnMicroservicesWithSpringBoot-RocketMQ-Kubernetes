# Learn Microservices with Spring Boot — Spring Cloud Alibaba with RocketMQ

This repository contains the source code of the practical use case described in the book [Learn Microservices with Spring Boot 3 (3rd Edition)](https://link.springer.com/book/10.1007/978-1-4842-9757-5).
The book follows a pragmatic approach to building a Microservice Architecture. You start with a small monolith and examine the pros and cons that come with a move to microservices.

This branch is a Spring Boot microservices system with centralized logs, distributed tracing, and Kubernetes/Helm deployment assets.

The figure below shows a high-level overview of the final version of our system.

![Logical View - Chapter 8 (Final)](resources/microservice_patterns-Config-Server-1.png)

## What's included

Core services:
- `multiplication` — challenge generation and answer checking
- `gamification` — scoring and leaderboard logic
- `gateway` — Spring Cloud Gateway with Sentinel flow control
- `logs` — centralized logging with RocketMQ
- `challenges-frontend` — React UI

Platform components:
- Nacos for service discovery and configuration
- RocketMQ for async messaging and logging
- OpenTelemetry for traces and metrics
- Prometheus, Grafana, Tempo, Loki, Elasticsearch, and Kibana for observability
- Helm/Kubernetes manifests under `k8s/`

## Quick start

### 1. Clone external dependencies

The Nacos Helm chart is **not bundled** in this repository. Clone it before running `helmfile sync`:

```bash
git clone https://github.com/nacos-group/nacos-k8s.git k8s/external/nacos-k8s
```

### 2. Create a kind cluster

From the repository root, run:

```bash
kind create cluster --config kind/kind-config.yaml
```

Run this command from the **repository root** because the config file uses relative paths such as `kind/kind/containerd-certs.d` and `kind/kind-config.yaml`.

### 3. Deploy to Kubernetes

From the `k8s/` directory:

```bash
helmfile sync
```

### Build services locally

```bash
cd multiplication && mvn clean package
cd ../gamification && mvn clean package
cd ../gateway && mvn clean package
cd ../logs && mvn clean package
```

### Build and test the frontend

```bash
cd challenges-frontend
npm install
npm test
npm run build
```

### Build Docker images

```bash
docker build -t multiplication:0.0.1-SNAPSHOT multiplication/
docker build -t gamification:0.0.1-SNAPSHOT gamification/
docker build -t gateway:0.0.1-SNAPSHOT gateway/
docker build -t logs:0.0.1-SNAPSHOT logs/
docker build -t challenges-frontend:1.0 challenges-frontend/
```

The local Docker Compose and Kubernetes MySQL setup both use `example/mysql:8.0.31`, built from `docker/image/mysql/8/Dockerfile`.

## Kubernetes

Cluster deployment, ingress setup, and the full observability drill-down architecture are documented in `k8s/README.md`.

Key manifests and charts live under:
- `k8s/charts/`
- `k8s/values/`
- `k8s/external/` (nacos-k8s must be cloned here first — see above)

## Access points

- Frontend: http://localhost
- API Gateway: http://localhost/api (with application url appended)
- Grafana: http://localhost/grafana
- Prometheus: http://localhost/prometheus
- Nacos: http://localhost/nacos/ (nacos/nacos)
- Sentinel Dashboard: http://localhost/sentinel-dashboard/ (sentinel/sentinel)
- Jaeger: http://localhost/jaeger
- Zipkin: http://localhost/zipkin
- Kibana: https://localhost/kibana

## Grafana AI Assistant — Grot

Grafana provides an official AI assistant called **Grot** that answers Grafana-related questions in natural language:

- [https://grafana.com/grot/](https://grafana.com/grot/?chat=hello&from=%2Fzh-cn%2Fgrafana%2F)

**What it can do:**
- Explain PromQL / LogQL / TraceQL query syntax
- Help design and debug Grafana dashboards
- Answer questions about Grafana configuration, plugins, and data sources
- Suggest alert rules and panel types

**Example use cases:**
```
"How do I write a query to show P95 latency?"
"Why does this PromQL expression return no data?"
"How do I configure trace-to-metrics linking between Tempo and Prometheus?"
```

Grot is trained on Grafana documentation, making it more accurate than general-purpose AI for anything in the Grafana observability ecosystem.

## Notes

- RocketMQ topics such as `attempts-topic` and `logs` need FIFO configuration in k8s.
- Gateway routes are defined in `gateway/src/main/resources/application.yml`.
- Grafana is configured to run under `/grafana`.
- If you need the Kubernetes workflow, use the dedicated `k8s/README.md` guide.

## About the book

If you want the background for this demo, the repository follows the book's step-by-step microservices case study and shows the transition from a small application to a distributed system.
