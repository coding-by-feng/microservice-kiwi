#!/bin/bash
# Deployment script for Kiwi Monolith
# Usage: ./deploy.sh [dev|test|prod] [options]
#
# Options:
#   --restart    Kill running instance and restart
#   --kill       Kill running instance only
#   --status     Show running status
#   --skip-build Skip build step
#   --debug      Enable debug mode

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$PROJECT_DIR/.." && pwd)"
PID_FILE="$SCRIPT_DIR/.kiwi-monolith.pid"
LOG_FILE="$SCRIPT_DIR/kiwi-monolith.log"

# Default values
ENV=${1:-dev}
RESTART=false
KILL_ONLY=false
STATUS_ONLY=false
SKIP_BUILD=false
DEBUG=false

# Parse arguments
for arg in "$@"; do
    case $arg in
        --restart)
            RESTART=true
            ;;
        --kill)
            KILL_ONLY=true
            ;;
        --status)
            STATUS_ONLY=true
            ;;
        --skip-build)
            SKIP_BUILD=true
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

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Get running PID
get_running_pid() {
    # First check PID file
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "$pid"
            return
        fi
    fi
    # Fallback: find by process name
    pgrep -f "kiwi-monolith.*\.jar" 2>/dev/null | head -1
}

# Kill running instance
kill_instance() {
    local pid=$(get_running_pid)
    if [ -n "$pid" ]; then
        log_info "Killing running instance (PID: $pid)..."
        kill "$pid" 2>/dev/null || true
        sleep 2
        # Force kill if still running
        if ps -p "$pid" > /dev/null 2>&1; then
            log_warn "Process still running, force killing..."
            kill -9 "$pid" 2>/dev/null || true
            sleep 1
        fi
        rm -f "$PID_FILE"
        log_info "Instance stopped."
    else
        log_info "No running instance found."
    fi
}

# Show status
show_status() {
    local pid=$(get_running_pid)
    if [ -n "$pid" ]; then
        echo -e "${GREEN}Status: RUNNING${NC}"
        echo "  PID: $pid"
        echo "  Uptime: $(ps -o etime= -p "$pid" 2>/dev/null | xargs)"
        echo "  Memory: $(ps -o rss= -p "$pid" 2>/dev/null | awk '{printf "%.1f MB", $1/1024}')"
    else
        echo -e "${RED}Status: STOPPED${NC}"
    fi
}

# Status only
if [ "$STATUS_ONLY" = true ]; then
    show_status
    exit 0
fi

# Kill only
if [ "$KILL_ONLY" = true ]; then
    kill_instance
    exit 0
fi

# Restart mode - kill first
if [ "$RESTART" = true ]; then
    kill_instance
fi

echo ""
echo "=========================================="
echo "  Kiwi Monolith Deployment"
echo "  Environment: $ENV"
echo "=========================================="
echo ""

# Check if already running (unless restart mode)
if [ "$RESTART" != true ]; then
    existing_pid=$(get_running_pid)
    if [ -n "$existing_pid" ]; then
        log_warn "Application is already running (PID: $existing_pid)"
        log_warn "Use --restart to kill and restart, or --kill to stop"
        exit 1
    fi
fi

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

# Step 2: Build application
if [ "$SKIP_BUILD" != true ]; then
    log_step "Building application..."
    cd "$ROOT_DIR"
    mvn clean package -pl kiwi-monolith -am -DskipTests -q
    log_info "Build successful!"
else
    log_info "Skipping build (--skip-build)"
fi

# Step 3: Find JAR
JAR_FILE=$(find "$PROJECT_DIR/target" -name "*.jar" -type f 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    log_error "Build failed - no JAR file found"
    exit 1
fi

log_info "JAR file: $JAR_FILE"

# Step 4: Load environment
ENV_FILE="$SCRIPT_DIR/.env.$ENV"
if [ -f "$ENV_FILE" ]; then
    log_info "Loading environment from $ENV_FILE"
    set -a
    source "$ENV_FILE"
    set +a
fi

# Step 5: Set up JVM options
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-$ENV}
JVM_OPTS=${JVM_OPTS:-"-Xms256m -Xmx512m"}
JVM_OPTS="$JVM_OPTS -Duser.timezone=${TZ:-Pacific/Auckland}"

if [ "$DEBUG" = true ]; then
    JVM_OPTS="$JVM_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    log_info "Debug mode enabled on port 5005"
fi

# Step 6: Export environment variables
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

# Step 7: Run application
log_step "Starting application..."
echo "----------------------------------------"

cd "$PROJECT_DIR"
nohup java $JVM_OPTS -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
APP_PID=$!
echo "$APP_PID" > "$PID_FILE"

sleep 2

if ps -p "$APP_PID" > /dev/null 2>&1; then
    log_info "Application started successfully!"
    log_info "PID: $APP_PID"
    log_info "Log file: $LOG_FILE"
    log_info ""
    log_info "Useful commands:"
    log_info "  View logs:    tail -f $LOG_FILE"
    log_info "  Stop:         $0 --kill"
    log_info "  Restart:      $0 $ENV --restart"
    log_info "  Status:       $0 --status"
else
    log_error "Application failed to start. Check logs:"
    tail -20 "$LOG_FILE"
    exit 1
fi
