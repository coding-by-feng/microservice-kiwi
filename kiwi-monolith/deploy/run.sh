#!/bin/bash
# Application runner script for Kiwi Monolith
# Usage: ./run.sh [dev|test|prod] [--build] [--debug]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default values
ENV=${1:-dev}
BUILD=false
DEBUG=false
DEBUG_PORT=5005

# Parse arguments
for arg in "$@"; do
    case $arg in
        --build)
            BUILD=true
            ;;
        --debug)
            DEBUG=true
            ;;
        dev|test|prod)
            ENV=$arg
            ;;
    esac
done

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

# Load environment file
ENV_FILE="$SCRIPT_DIR/.env.$ENV"
if [ -f "$ENV_FILE" ]; then
    log_info "Loading environment from $ENV_FILE"
    set -a
    source "$ENV_FILE"
    set +a
else
    log_warn "Environment file $ENV_FILE not found, using defaults"
fi

# Also load .env if exists (for overrides)
if [ -f "$SCRIPT_DIR/.env" ]; then
    log_info "Loading overrides from .env"
    set -a
    source "$SCRIPT_DIR/.env"
    set +a
fi

# Set Spring profile
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-$ENV}

log_info "Environment: ${BLUE}$ENV${NC}"
log_info "Spring Profile: ${BLUE}$SPRING_PROFILES_ACTIVE${NC}"

# Build if requested
if [ "$BUILD" = true ]; then
    log_info "Building application..."
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests -q
    log_info "Build complete!"
fi

# Find JAR file
JAR_FILE=$(find "$PROJECT_DIR/target" -name "*.jar" -type f 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
    log_error "No JAR file found in target directory. Run with --build flag or build manually."
    exit 1
fi

log_info "Using JAR: $JAR_FILE"

# JVM Options
JVM_OPTS=${JVM_OPTS:-"-Xms256m -Xmx512m"}

if [ "$DEBUG" = true ]; then
    JVM_OPTS="$JVM_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"
    log_info "Debug mode enabled on port $DEBUG_PORT"
fi

# Timezone
JVM_OPTS="$JVM_OPTS -Duser.timezone=${TZ:-Pacific/Auckland}"

# Export environment variables for Spring Boot
export DB_HOST_DEV=${DB_HOST:-localhost}
export DB_PORT_DEV=${DB_PORT:-3307}
export DB_NAME_DEV=${DB_NAME:-kiwi_db}
export DB_USERNAME_DEV=${DB_USERNAME:-root}
export DB_PASSWORD_DEV=${DB_PASSWORD:-kiwi123}
export REDIS_HOST_DEV=${REDIS_HOST:-localhost}
export REDIS_PORT_DEV=${REDIS_PORT:-6380}
export REDIS_PASSWORD_DEV=${REDIS_PASSWORD:-kiwi123}
export ES_URIS_DEV=${ES_URIS:-http://localhost:9201}
export ES_USERNAME_DEV=${ES_USERNAME:-elastic}
export ES_PASSWORD_DEV=${ES_PASSWORD:-changeme}
export FASTDFS_TRACKER_DEV=${FASTDFS_TRACKER:-localhost:22122}

# Run the application
log_info "Starting Kiwi Monolith..."
echo "----------------------------------------"

cd "$PROJECT_DIR"
java $JVM_OPTS -jar "$JAR_FILE"
