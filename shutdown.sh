#!/bin/bash

YELLOW='\033[1;33m'
NC='\033[0m'
echo -e "${YELLOW}[INFO] shutdown current local deployment...${NC}"
cd ..
docker rm -f zeebe operate elasticsearch tasklist connectors jaeger otel-job-worker
echo -e "${YELLOW}[INFO] Camunda Opentelemetry stack shut down successfully!${NC}"
