#!/usr/bin/env bash
set -euo pipefail

echo "=================================================="
echo " Starting Deployment for StormAPI"
echo "=================================================="

DEPLOY_DIR="/opt/stormapi"

if [ ! -d "$DEPLOY_DIR" ]; then
    echo "ERROR: Directory $DEPLOY_DIR does not exist. Please run setup-vm.sh first." >&2
    exit 1
fi

cd "$DEPLOY_DIR"

echo "==> Pulling latest container images..."
docker compose -f docker-compose.prod.yml pull

echo "==> Starting production containers..."
docker compose -f docker-compose.prod.yml up -d

echo "==> Cleaning up unused Docker images..."
docker image prune -f

echo "==> Running backend health check..."
MAX_RETRIES=30
RETRY_INTERVAL=2
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"
HEALTHY=false

for ((i=1; i<=MAX_RETRIES; i++)); do
    if curl -sf "$HEALTH_CHECK_URL" > /dev/null 2>&1; then
        echo "==> Health check PASSED on attempt $i/$MAX_RETRIES! Backend is UP."
        HEALTHY=true
        break
    else
        echo "Waiting for backend health check ($HEALTH_CHECK_URL)... (attempt $i/$MAX_RETRIES)"
        sleep "$RETRY_INTERVAL"
    fi
done

if [ "$HEALTHY" = false ]; then
    echo "ERROR: Backend health check failed after $MAX_RETRIES attempts." >&2
    exit 1
fi

echo "=================================================="
echo " Deployment process completed successfully!"
echo "=================================================="
