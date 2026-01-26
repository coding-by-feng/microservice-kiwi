#!/bin/bash
# Deployment script for Kiwi Monolith
# Usage: ./deploy.sh [command]
#   Commands:
#     build     - Build the JAR only
#     kill      - Kill the running application
#     start     - Start the application (requires JAR)
#     restart   - Kill + Start
#     deploy    - Build + Start
#     redeploy  - Build + Kill + Start (full redeploy)
#     status    - Show application status

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT_DIR="$(cd "$PROJECT_DIR/.." && pwd)"

# Configuration
PROFILE="prod"
LOG_FILE="$HOME/app.log"
PID_FILE="$HOME/kiwi-monolith.pid"
APP_NAME="kiwi-monolith"

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

show_usage() {
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  build      Build the JAR only"
    echo "  kill       Kill the running application"
    echo "  start      Start the application (requires JAR)"
    echo "  restart    Kill + Start"
    echo "  deploy     Build + Start"
    echo "  redeploy   Build + Kill + Start (full redeploy)"
    echo "  status     Show application status"
    echo ""
    echo "All commands run with profile: $PROFILE"
    echo "Log file: $LOG_FILE"
    echo ""
}

check_java() {
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed!"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        log_error "Java 17+ is required. Current version: $JAVA_VERSION"
        exit 1
    fi
    log_info "Java version: OK ($JAVA_VERSION)"
}

find_jar() {
    JAR_FILE=$(find "$PROJECT_DIR/target" -name "*.jar" -type f 2>/dev/null | head -1)
    if [ -z "$JAR_FILE" ]; then
        log_error "No JAR file found in $PROJECT_DIR/target"
        log_error "Run '$0 build' first"
        exit 1
    fi
    echo "$JAR_FILE"
}

get_pid() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "$PID"
            return
        fi
    fi
    # Fallback: find by process name
    pgrep -f "$APP_NAME.*\.jar" 2>/dev/null || true
}

do_build() {
    log_step "Building application..."

    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed!"
        exit 1
    fi

    cd "$ROOT_DIR"
    mvn clean package -pl kiwi-monolith -am -DskipTests -q

    JAR_FILE=$(find_jar)
    log_info "Build successful: $JAR_FILE"
}

do_kill() {
    log_step "Stopping application..."

    PID=$(get_pid)
    if [ -z "$PID" ]; then
        log_info "Application is not running"
        return
    fi

    log_info "Killing process $PID..."
    kill "$PID" 2>/dev/null || true

    # Wait for graceful shutdown
    for i in {1..10}; do
        if ! ps -p "$PID" > /dev/null 2>&1; then
            break
        fi
        sleep 1
    done

    # Force kill if still running
    if ps -p "$PID" > /dev/null 2>&1; then
        log_warn "Force killing process $PID..."
        kill -9 "$PID" 2>/dev/null || true
    fi

    rm -f "$PID_FILE"
    log_info "Application stopped"
}

do_start() {
    log_step "Starting application..."

    check_java

    PID=$(get_pid)
    if [ -n "$PID" ]; then
        log_error "Application is already running (PID: $PID)"
        log_error "Run '$0 kill' first or use '$0 restart'"
        exit 1
    fi

    JAR_FILE=$(find_jar)
    log_info "JAR: $JAR_FILE"
    log_info "Profile: $PROFILE"
    log_info "Log: $LOG_FILE"

    nohup java -jar "$JAR_FILE" --spring.profiles.active="$PROFILE" > "$LOG_FILE" 2>&1 &
    NEW_PID=$!
    echo "$NEW_PID" > "$PID_FILE"

    log_info "Application started (PID: $NEW_PID)"
    log_info "Tail logs: tail -f $LOG_FILE"
}

do_status() {
    log_step "Application status"

    PID=$(get_pid)
    if [ -z "$PID" ]; then
        log_info "Application is NOT running"
    else
        log_info "Application is running (PID: $PID)"
        log_info "Log file: $LOG_FILE"
    fi
}

# Main
COMMAND=${1:-help}

echo ""
echo "=========================================="
echo "  Kiwi Monolith Deployment"
echo "  Profile: $PROFILE"
echo "=========================================="
echo ""

case "$COMMAND" in
    build)
        do_build
        ;;
    kill|stop)
        do_kill
        ;;
    start)
        do_start
        ;;
    restart)
        do_kill
        do_start
        ;;
    deploy)
        do_build
        do_start
        ;;
    redeploy)
        do_build
        do_kill
        do_start
        ;;
    status)
        do_status
        ;;
    help|--help|-h|*)
        show_usage
        ;;
esac
