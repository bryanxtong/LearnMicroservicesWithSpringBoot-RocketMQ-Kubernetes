# Learn Microservices with Spring Boot -（Spring Cloud Alibaba with RocketMQ）
This repository contains the source code of the practical use case described in the book [Learn Microservices with Spring Boot 3 (3rd Edition)](https://link.springer.com/book/10.1007/978-1-4842-9757-5).
The book follows a pragmatic approach to building a Microservice Architecture. You start with a small monolith and examine the pros and cons that come with a move to microservices.

This branch is a Spring Boot microservices system with centralized logs, distributed tracing, and Kubernetes/Helm deployment assets.

The figure below shows a high-level overview of the final version of our system.

![Logical View - Chapter 8 (Final)](resources/microservice_patterns-Config-Server-1.png)

## What’s included

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

### Run the full local stack

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

Cluster deployment and ingress setup are documented in `k8s/README.md`.

Key manifests and charts live under:
- `k8s/charts/`
- `k8s/values/`
- `k8s/templates/`

## Access points

- Frontend: http://localhost
- Grafana: http://localhost/grafana
- Prometheus: http://localhost/prometheus
- Nacos: http://localhost/nacos/ (nacos/nacos)
- Sentinel Dashboard: http://localhost/sentinel-dashboard/ (sentinel/sentinel)
- Kibana: https://localhost/kibana
- Jaeger: http://localhost/jaeger

## Notes

- RocketMQ topics such as `attempts-topic` and `logs` need FIFO configuration in k8s.
- Gateway routes are defined in `gateway/src/main/resources/application.yml`.
- Grafana is configured to run under `/grafana`.
- If you need the Kubernetes workflow, use the dedicated `k8s/README.md` guide.

## About the book

If you want the background for this demo, the repository follows the book’s step-by-step microservices case study and shows the transition from a small application to a distributed system.
