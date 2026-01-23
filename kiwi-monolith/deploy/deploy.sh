#!/bin/bash
# Deploy script for kiwi-monolith
# Usage: ./deploy.sh [build|restart|kill|start]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONOLITH_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR_NAME="kiwi-monolith-3.0.0.jar"
JAR_PATH="$HOME/$JAR_NAME"
LOG_FILE="$HOME/app.log"

# Remote server config (for upload)
REMOTE_HOST="139.180.180.203"
REMOTE_USER="root"
REMOTE_DIR="~"

do_build() {
    echo "=== Building ==="
    cd "$MONOLITH_DIR"
    mvn clean package -DskipTests -q
    cp target/$JAR_NAME "$HOME/"
    echo "Build complete: $JAR_PATH"
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
    echo "=== Killing port 8088 ==="
    kill -9 $(lsof -t -i:8088) 2>/dev/null || echo "No process on 8088"
}

select_memory() {
    echo "=== Select Memory Configuration ==="
    echo "1) 215m (minimal)"
    echo "2) 512m (small)"
    echo "3) 1g (medium)"
    echo "4) 2g (large)"
    echo "5) 4g (extra large)"
    echo "6) Custom"
    echo ""
    read -p "Select [1-6]: " mem_choice
    case $mem_choice in
        1) JAVA_OPTS="-Xms215m -Xmx215m" ;;
        2) JAVA_OPTS="-Xms512m -Xmx512m" ;;
        3) JAVA_OPTS="-Xms1g -Xmx1g" ;;
        4) JAVA_OPTS="-Xms2g -Xmx2g" ;;
        5) JAVA_OPTS="-Xms4g -Xmx4g" ;;
        6)
            read -p "Enter Xms (e.g., 256m, 1g): " xms
            read -p "Enter Xmx (e.g., 512m, 2g): " xmx
            JAVA_OPTS="-Xms$xms -Xmx$xmx"
            ;;
        *) JAVA_OPTS="-Xms512m -Xmx512m" ;;
    esac
    echo "Using: $JAVA_OPTS"
}

do_start() {
    echo "=== Starting ==="
    if [ -z "$JAVA_OPTS" ]; then
        select_memory
    fi
    cd "$HOME"
    nohup java $JAVA_OPTS -jar "$JAR_PATH" > "$LOG_FILE" 2>&1 &
    echo "Waiting for startup... (tail -f $LOG_FILE)"
    echo "Press Ctrl+C to stop watching logs (app will continue running)"
    sleep 2
    tail -f "$LOG_FILE"
}

case "${1:-menu}" in
    build)
        do_build
        ;;
    upload)
        do_build
        do_upload
        ;;
    deploy)
        select_memory
        do_build
        do_kill
        do_start
        ;;
    kill)
        do_kill
        ;;
    start)
        select_memory
        do_start
        ;;
    restart)
        select_memory
        do_kill
        do_start
        ;;
    menu|*)
        echo "=== Kiwi Monolith Deploy ==="
        echo "1) Build only"
        echo "2) Build and upload to remote"
        echo "3) Build, kill, and start (full deploy)"
        echo "4) Kill (stop app on port 8088)"
        echo "5) Start (run jar with nohup)"
        echo "6) Restart (kill + start)"
        echo ""
        read -p "Select [1-6]: " choice
        # Prompt for memory upfront if action includes start
        case $choice in
            3|5|6) select_memory ;;
        esac
        case $choice in
            1) do_build ;;
            2) do_build; do_upload ;;
            3) do_build; do_kill; do_start ;;
            4) do_kill ;;
            5) do_start ;;
            6) do_kill; do_start ;;
            *) echo "Invalid choice" ;;
        esac
        ;;
esac
