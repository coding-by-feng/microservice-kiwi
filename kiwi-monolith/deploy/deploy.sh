#!/bin/bash
# Deploy script for kiwi-monolith
# Usage: ./deploy.sh [build|restart|kill|start]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONOLITH_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR_NAME="kiwi-monolith-3.0.0.jar"
LOG_FILE="app.log"

# Remote server config (for upload)
REMOTE_HOST="139.180.180.203"
REMOTE_USER="root"
REMOTE_DIR="~"

do_build() {
    echo "=== Building ==="
    cd "$MONOLITH_DIR"
    mvn clean package -DskipTests -q
    echo "Build complete: $(find target -name '*.jar' -type f | head -1)"
}

do_upload() {
    echo "=== Uploading ==="
    JAR_FILE=$(find "$MONOLITH_DIR/target" -name "*.jar" -type f | head -1)
    if [ -z "$JAR_FILE" ]; then
        echo "ERROR: No JAR file found. Run build first."
        exit 1
    fi
    scp -O "$JAR_FILE" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/"
    echo "Uploaded: $(basename "$JAR_FILE")"
}

do_kill() {
    echo "=== Killing port 8080 ==="
    kill -9 $(lsof -t -i:8080) 2>/dev/null || echo "No process on 8080"
}

do_start() {
    echo "=== Starting ==="
    nohup java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &
    sleep 2
    if lsof -i:8080 > /dev/null 2>&1; then
        echo "Started! PID: $(lsof -t -i:8080)"
    else
        echo "Failed to start. Check $LOG_FILE"
    fi
}

case "${1:-menu}" in
    build)
        do_build
        ;;
    upload)
        do_build
        do_upload
        ;;
    kill)
        do_kill
        ;;
    start)
        do_start
        ;;
    restart)
        do_kill
        do_start
        ;;
    menu|*)
        echo "=== Kiwi Monolith Deploy ==="
        echo "1) Build only"
        echo "2) Build and upload to remote"
        echo "3) Kill (stop app on port 8080)"
        echo "4) Start (run jar with nohup)"
        echo "5) Restart (kill + start)"
        echo ""
        read -p "Select [1-5]: " choice
        case $choice in
            1) do_build ;;
            2) do_build; do_upload ;;
            3) do_kill ;;
            4) do_start ;;
            5) do_kill; do_start ;;
            *) echo "Invalid choice" ;;
        esac
        ;;
esac
