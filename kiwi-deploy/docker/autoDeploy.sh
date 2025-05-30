#!/bin/bash

set -e  # Exit on any error

echo "=== KIWI MICROSERVICE DEPLOYMENT WITH DOCKER COMPOSE ==="

# Cleanup function
cleanup_existing() {
    echo "Cleaning up existing deployment..."

    # Stop existing services (ignore errors if they don't exist)
    docker compose --project-name kiwi-service -f ~/microservice-kiwi/kiwi-deploy/docker/docker-compose-service.yml down --remove-orphans 2>/dev/null || true
    docker compose --project-name kiwi-base -f ~/microservice-kiwi/kiwi-deploy/docker/docker-compose-base.yml down --remove-orphans 2>/dev/null || true

    echo "Cleanup completed"
}

# Build images
build_images() {
    echo "Building Docker images..."
    cp ~/gcp-credentials.json  ~/docker/kiwi/upms/
    cp ~/gcp-credentials.json  ~/docker/kiwi/gate/
    cp ~/gcp-credentials.json ~/docker/kiwi/word/
    cp ~/gcp-credentials.json ~/docker/kiwi/crawler/
    cp ~/gcp-credentials.json  ~/docker/kiwi/ai/biz
    cp ~/gcp-credentials.json  ~/docker/kiwi/ai/batch


    docker build -f ~/docker/kiwi/eureka/Dockerfile -t kiwi-eureka:2.0 ~/docker/kiwi/eureka/
    docker build -f ~/docker/kiwi/config/Dockerfile -t kiwi-config:2.0 ~/docker/kiwi/config/
    docker build -f ~/docker/kiwi/upms/Dockerfile -t kiwi-upms:2.0 ~/docker/kiwi/upms/
    docker build -f ~/docker/kiwi/auth/Dockerfile -t kiwi-auth:2.0 ~/docker/kiwi/auth/
    docker build -f ~/docker/kiwi/gate/Dockerfile -t kiwi-gate:2.0 ~/docker/kiwi/gate/
    docker build -f ~/docker/kiwi/word/biz/Dockerfile -t kiwi-word-biz:2.0 ~/docker/kiwi/word/
    docker build -f ~/docker/kiwi/crawler/Dockerfile -t kiwi-crawler:2.0 ~/docker/kiwi/crawler/
    docker build -f ~/docker/kiwi/ai/biz/Dockerfile -t kiwi-ai-biz:2.0 ~/docker/kiwi/ai/biz
    docker build -f ~/docker/kiwi/ai/batch/Dockerfile -t kiwi-ai-biz-batch:2.0 ~/docker/kiwi/ai/batch

    echo "All images built successfully"
}

# Deploy services
deploy_services() {
    echo "Starting base services..."
    docker compose --project-name kiwi-base -f ~/microservice-kiwi/kiwi-deploy/docker/docker-compose-base.yml up -d --remove-orphans --build

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
    docker compose --project-name kiwi-service -f ~/microservice-kiwi/kiwi-deploy/docker/docker-compose-service.yml up -d --force-recreate --remove-orphans --build

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
    docker compose --project-name kiwi-base -f ~/microservice-kiwi/kiwi-deploy/docker/docker-compose-base.yml ps

    echo -e "\nApplication services status:"
    docker compose --project-name kiwi-service -f ~/microservice-kiwi/kiwi-deploy/docker/docker-compose-service.yml ps
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