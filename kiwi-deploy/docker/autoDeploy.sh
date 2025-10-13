#!/bin/bash
set -e
set -o pipefail

# Short-circuit for OBM/OBMAS (build-only) modes to prevent any docker actions
MODE_ARG="${1:-}"
if [[ "$MODE_ARG" == "-mode=obm" || "$MODE_ARG" == "-mode=obmas" ]]; then
  echo "OBM/OBMAS mode detected in autoDeploy.sh — skipping docker image build and deployment."
  exit 0
fi

# ---------------------------------------------------------
# Dynamic path resolution (replaces unsafe use of ~ under sudo)
# ---------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# autoDeploy.sh is in: microservice-kiwi/kiwi-deploy/docker/
# project root: three levels up from kiwi-deploy/docker (adjust if layout changes)
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
DOCKER_DEPLOY_DIR="$PROJECT_ROOT/microservice-kiwi/kiwi-deploy/docker"
COMPOSE_BASE_YML="$DOCKER_DEPLOY_DIR/docker-compose-base.yml"
COMPOSE_SERVICE_YML="$DOCKER_DEPLOY_DIR/docker-compose-service.yml"
DOCKER_KIWI_ROOT="$PROJECT_ROOT/docker/kiwi"

# Detect docker compose command
if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
elif docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD="docker compose"
else
  echo "❌ Neither 'docker compose' nor 'docker-compose' found in PATH."
  exit 1
fi

# Preflight checks
missing=0
for f in "$COMPOSE_BASE_YML" "$COMPOSE_SERVICE_YML"; do
  if [ ! -f "$f" ]; then
     echo "❌ Missing compose file: $f"
     ((missing++))
  fi
done
for d in eureka config upms auth gate word crawler ai; do
  if [ ! -d "$DOCKER_KIWI_ROOT/$d" ]; then
     echo "❌ Missing expected Docker build directory: $DOCKER_KIWI_ROOT/$d"
     ((missing++))
  fi
done
if [ $missing -gt 0 ]; then
  echo "Aborting due to $missing missing required file(s)/directory(ies)."
  echo "PROJECT_ROOT resolved to: $PROJECT_ROOT"
  exit 1
fi

echo "=== KIWI MICROSERVICE DEPLOYMENT WITH DOCKER COMPOSE ==="
echo "PROJECT_ROOT: $PROJECT_ROOT"
echo "Using compose command: $COMPOSE_CMD"
echo "Base compose: $COMPOSE_BASE_YML"
echo "Service compose: $COMPOSE_SERVICE_YML"

# Cleanup function
cleanup_existing() {
    echo "Cleaning up existing deployment..."

    # Stop existing services (ignore errors if they don't exist)
    $COMPOSE_CMD --project-name kiwi-service -f "$COMPOSE_SERVICE_YML" down --remove-orphans 2>/dev/null || true
    $COMPOSE_CMD --project-name kiwi-base -f "$COMPOSE_BASE_YML" down --remove-orphans 2>/dev/null || true

    echo "Cleanup completed"
}

# Build images
build_images() {
    echo "Building Docker images..."
    echo "This process will build all microservice components for the Kiwi platform"
    echo "Estimated time: 5-15 minutes depending on your system"
    echo

    echo "📡 Building Eureka Service Discovery Server..."
    docker build -f "$DOCKER_KIWI_ROOT/eureka/Dockerfile" -t kiwi-eureka:2.0 "$DOCKER_KIWI_ROOT/eureka/"
    echo "   ✅ Eureka server built successfully"
    echo

    echo "⚙️  Building Configuration Management Service..."
    docker build -f "$DOCKER_KIWI_ROOT/config/Dockerfile" -t kiwi-config:2.0 "$DOCKER_KIWI_ROOT/config/"
    echo "   ✅ Config service built successfully"
    echo

    echo "👥 Building User Permission Management System (UPMS)..."
    docker build -f "$DOCKER_KIWI_ROOT/upms/Dockerfile" -t kiwi-upms:2.0 "$DOCKER_KIWI_ROOT/upms/"
    echo "   ✅ UPMS service built successfully"
    echo

    echo "🔐 Building Authentication Service..."
    docker build -f "$DOCKER_KIWI_ROOT/auth/Dockerfile" -t kiwi-auth:2.0 "$DOCKER_KIWI_ROOT/auth/"
    echo "   ✅ Auth service built successfully"
    echo

    echo "🚪 Building API Gateway Service..."
    docker build -f "$DOCKER_KIWI_ROOT/gate/Dockerfile" -t kiwi-gate:2.0 "$DOCKER_KIWI_ROOT/gate/"
    echo "   ✅ Gateway service built successfully"
    echo

    echo "📝 Building Word Processing Business Service..."
    docker build -f "$DOCKER_KIWI_ROOT/word/biz/Dockerfile" -t kiwi-word-biz:2.0 "$DOCKER_KIWI_ROOT/word/"
    echo "   ✅ Word processing service built successfully"
    echo

    echo "🕷️  Building Web Crawler Service..."
    docker build -f "$DOCKER_KIWI_ROOT/crawler/Dockerfile" -t kiwi-crawler:2.0 "$DOCKER_KIWI_ROOT/crawler/"
    echo "   ✅ Crawler service built successfully"
    echo

    echo "🤖 Building AI Business Logic Service..."
    docker build -f "$DOCKER_KIWI_ROOT/ai/biz/Dockerfile" -t kiwi-ai-biz:2.0 "$DOCKER_KIWI_ROOT/ai/biz"
    echo "   ✅ AI business service built successfully"
    echo

    echo "⚡ Building AI Batch Processing Service..."
    docker build -f "$DOCKER_KIWI_ROOT/ai/batch/Dockerfile" -t kiwi-ai-biz-batch:2.0 "$DOCKER_KIWI_ROOT/ai/batch"
    echo "   ✅ AI batch service built successfully"
    echo

    echo "🎉 All images built successfully!"
    echo "Total services built: 9 microservices ready for deployment"
}

# Deploy services
deploy_services() {
    echo "Starting base services..."
    $COMPOSE_CMD --project-name kiwi-base -f "$COMPOSE_BASE_YML" up -d --remove-orphans --build

    # Check if eureka is responding (any HTTP status means it's up)
    until curl -s http://localhost:8762/health >/dev/null 2>&1; do
      echo "Waiting for Eureka..."
      sleep 10
    done

    # Check if config service is responding
    until curl -s http://localhost:7771/health >/dev/null 2>&1; do
      echo "Waiting for Config Service..."
      sleep 10
    done

    echo "Starting application services..."
    $COMPOSE_CMD --project-name kiwi-service -f "$COMPOSE_SERVICE_YML" up -d --force-recreate --remove-orphans --build

    echo "Application services started successfully"
}

# Stop crawler
stop_crawler() {
    echo "Stopping crawler service..."

    # More flexible crawler detection
    crawler_containers=$(docker ps --format "{{.Names}}" | grep -i crawler || true)

    if [ -n "$crawler_containers" ]; then
        echo "Found crawler containers: $crawler_containers"
        echo "$crawler_containers" | xargs -r docker stop
        echo "Crawler containers stopped"
    else
        echo "No crawler containers found running"
    fi
}

# Show deployment status
show_status() {
    echo -e "\n=== DEPLOYMENT STATUS ==="

    echo "Running containers:"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

    echo -e "\nBase services status:"
    $COMPOSE_CMD --project-name kiwi-base -f "$COMPOSE_BASE_YML" ps

    echo -e "\nApplication services status:"
    $COMPOSE_CMD --project-name kiwi-service -f "$COMPOSE_SERVICE_YML" ps
}

# Main execution
main() {
    cleanup_existing
    build_images
    deploy_services
    stop_crawler
    show_status

    echo -e "\n=== DEPLOYMENT COMPLETED SUCCESSFULLY ==="
}

# Error handling
trap 'echo "ERROR: Deployment failed at line $LINENO. Check the logs above."' ERR

# Run main function
main