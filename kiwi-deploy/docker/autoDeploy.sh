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
    echo "This process will build all microservice components for the Kiwi platform"
    echo "Estimated time: 5-15 minutes depending on your system"
    echo

    echo "📡 Building Eureka Service Discovery Server..."
    echo "   → Service registry for microservice discovery and health monitoring"
    docker build -f ~/docker/kiwi/eureka/Dockerfile -t kiwi-eureka:2.0 ~/docker/kiwi/eureka/
    echo "   ✅ Eureka server built successfully"
    echo

    echo "⚙️  Building Configuration Management Service..."
    echo "   → Centralized configuration server for all microservices"
    docker build -f ~/docker/kiwi/config/Dockerfile -t kiwi-config:2.0 ~/docker/kiwi/config/
    echo "   ✅ Config service built successfully"
    echo

    echo "👥 Building User Permission Management System (UPMS)..."
    echo "   → User authentication, authorization, and permission management"
    docker build -f ~/docker/kiwi/upms/Dockerfile -t kiwi-upms:2.0 ~/docker/kiwi/upms/
    echo "   ✅ UPMS service built successfully"
    echo

    echo "🔐 Building Authentication Service..."
    echo "   → OAuth2/JWT token generation and validation service"
    docker build -f ~/docker/kiwi/auth/Dockerfile -t kiwi-auth:2.0 ~/docker/kiwi/auth/
    echo "   ✅ Auth service built successfully"
    echo

    echo "🚪 Building API Gateway Service..."
    echo "   → Request routing, load balancing, and API rate limiting"
    docker build -f ~/docker/kiwi/gate/Dockerfile -t kiwi-gate:2.0 ~/docker/kiwi/gate/
    echo "   ✅ Gateway service built successfully"
    echo

    echo "📝 Building Word Processing Business Service..."
    echo "   → Document processing, text analysis, and content management"
    docker build -f ~/docker/kiwi/word/biz/Dockerfile -t kiwi-word-biz:2.0 ~/docker/kiwi/word/
    echo "   ✅ Word processing service built successfully"
    echo

    echo "🕷️  Building Web Crawler Service..."
    echo "   → Data extraction, web scraping, and content indexing"
    docker build -f ~/docker/kiwi/crawler/Dockerfile -t kiwi-crawler:2.0 ~/docker/kiwi/crawler/
    echo "   ✅ Crawler service built successfully"
    echo

    echo "🤖 Building AI Business Logic Service..."
    echo "   → Machine learning models, AI processing, and intelligent features"
    docker build -f ~/docker/kiwi/ai/biz/Dockerfile -t kiwi-ai-biz:2.0 ~/docker/kiwi/ai/biz
    echo "   ✅ AI business service built successfully"
    echo

    echo "⚡ Building AI Batch Processing Service..."
    echo "   → Background AI tasks, bulk processing, and scheduled ML jobs"
    docker build -f ~/docker/kiwi/ai/batch/Dockerfile -t kiwi-ai-biz-batch:2.0 ~/docker/kiwi/ai/batch
    echo "   ✅ AI batch service built successfully"
    echo

    echo "🎉 All images built successfully!"
    echo "Total services built: 9 microservices ready for deployment"
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