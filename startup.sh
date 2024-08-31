#!/bin/bash

YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}[INFO] building JAR artifact...${NC}"
cd otel-interceptor
mvn clean package
echo -e "${YELLOW}[INFO] building Zeebe Docker image...${NC}"
docker build -t zeebe-otel/zeebe:1.0.0 .
echo -e "${YELLOW}[INFO] building Job Worker Go Docker image...${NC}"
cd ../otel-job-worker
docker build -t zeebe-otel/otel-job-worker:1.0.0 .
echo -e "${YELLOW}[INFO] shutdown current local deployment...${NC}"
cd ..
docker rm -f zeebe operate elasticsearch tasklist connectors jaeger otel-job-worker
echo -e "${YELLOW}[INFO] deploy local docker-compose...${NC}"
docker-compose -f docker/docker-compose.yaml up -d
echo -e "${YELLOW}[INFO] Camunda Opentelemetry stack deployed successfully!${NC}"
