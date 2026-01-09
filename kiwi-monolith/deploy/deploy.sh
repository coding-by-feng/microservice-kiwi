#!/bin/bash
# Deployment script for Kiwi Monolith
# Usage: ./deploy.sh [dev|test|prod]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$PROJECT_DIR/.." && pwd)"

# Default environment
ENV=${1:-dev}

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Load environment
ENV_FILE="$SCRIPT_DIR/.env.$ENV"
if [ -f "$ENV_FILE" ]; then
    set -a
    source "$ENV_FILE"
    set +a
fi

echo ""
echo "=========================================="
echo "  Kiwi Monolith Deployment"
echo "  Environment: $ENV"
echo "=========================================="
echo ""

# Step 1: Check prerequisites
log_step "Checking prerequisites..."

if ! command -v java &> /dev/null; then
    log_error "Java is not installed!"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    log_error "Java 17+ is required. Current version: $JAVA_VERSION"
    exit 1
fi
log_info "Java version: OK"

if ! command -v mvn &> /dev/null; then
    log_error "Maven is not installed!"
    exit 1
fi
log_info "Maven: OK"

if ! command -v docker &> /dev/null; then
    log_warn "Docker is not installed. Infrastructure services must be running externally."
else
    log_info "Docker: OK"
fi

# Step 2: Start infrastructure (dev only)
if [ "$ENV" = "dev" ]; then
    log_step "Starting infrastructure services..."
    "$SCRIPT_DIR/infra.sh" start
fi

# Step 3: Build application
log_step "Building application..."
cd "$ROOT_DIR"

# Build only the monolith module
mvn clean package -pl kiwi-monolith -am -DskipTests -q
log_info "Build successful!"

# Step 4: Find JAR
JAR_FILE=$(find "$PROJECT_DIR/target" -name "*.jar" -type f 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    log_error "Build failed - no JAR file found"
    exit 1
fi

log_info "JAR file: $JAR_FILE"

# Step 5: Run application
log_step "Starting application..."
"$SCRIPT_DIR/run.sh" "$ENV"
