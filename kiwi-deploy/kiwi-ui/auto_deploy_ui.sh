#!/bin/bash

# UI Deployment Script for Kiwi Docker Container
# This script updates the UI files and restarts the kiwi-ui container
# Modes:
#   -mode=d   : Deploy only (default behavior)
#   -mode=kp  : Kill process on port

set -e  # Exit on any error

# Default mode
MODE="d"
PORT=""

# Parse command line arguments
for arg in "$@"; do
    case $arg in
        -mode=*)
        MODE="${arg#*=}"
        shift
        ;;
        -port=*)
        PORT="${arg#*=}"
        shift
        ;;
        *)
        # Unknown option
        ;;
    esac
done

# Function to kill process on port
kill_process_on_port() {
    if [ -z "$PORT" ]; then
        echo "❌ Error: Port number required for kill process mode"
        echo "Usage: $0 -mode=kp -port=<port_number>"
        exit 1
    fi

    echo "🔍 Checking for processes on port $PORT..."

    # Find process using the port
    PID=$(lsof -ti :$PORT 2>/dev/null || true)

    if [ -z "$PID" ]; then
        echo "ℹ️  No process found running on port $PORT"
        return 0
    fi

    echo "🎯 Found process(es) with PID(s): $PID"

    # Get process details before killing
    echo "📋 Process details:"
    ps -p $PID -o pid,ppid,cmd 2>/dev/null || true

    # Kill the process(es)
    echo "🔫 Killing process(es) on port $PORT..."
    kill -9 $PID 2>/dev/null || true

    # Verify the process is killed
    sleep 1
    REMAINING_PID=$(lsof -ti :$PORT 2>/dev/null || true)
    if [ -z "$REMAINING_PID" ]; then
        echo "✅ Process(es) successfully killed on port $PORT"
    else
        echo "⚠️  Warning: Some processes may still be running on port $PORT"
    fi
}

# Function to deploy UI
deploy_ui() {
    echo "🚀 Starting UI deployment process..."

    # Get the actual user's home directory
    USER_HOME=$(getent passwd $SUDO_USER | cut -d: -f6 2>/dev/null || echo $HOME)

    # Step 1: Clean up existing UI dist directory first
    echo "🧹 Cleaning up existing UI distribution files..."
    if [ -d "$USER_HOME/docker/ui/dist" ]; then
        rm -rf "$USER_HOME/docker/ui/dist"
        echo "✅ Old dist directory removed"
    else
        echo "ℹ️  No existing dist directory found"
    fi

    # Step 2: Move dist.zip to docker directory
    echo "📦 Moving dist.zip to docker directory..."
    if [ ! -f "$USER_HOME/dist.zip" ]; then
        echo "❌ Error: $USER_HOME/dist.zip not found!"
        exit 1
    fi
    mv "$USER_HOME/dist.zip" "$USER_HOME/docker/ui"
    echo "✅ dist.zip moved successfully"

    # Step 3: Navigate to docker/ui directory and extract
    echo "📂 Navigating to docker/ui directory..."
    cd "$USER_HOME/docker/ui/"

    echo "📤 Extracting dist.zip..."
    if [ ! -f dist.zip ]; then
        echo "❌ Error: dist.zip not found in $USER_HOME/docker/ui/"
        exit 1
    fi
    unzip -q dist.zip
    echo "✅ Files extracted successfully"

    # Step 4: Clean up macOS metadata files
    echo "🧹 Removing macOS metadata files..."
    if [ -d __MACOSX/ ]; then
        rm -rf __MACOSX/
        echo "✅ __MACOSX directory removed"
    else
        echo "ℹ️  No __MACOSX directory found"
    fi

    # Step 5: Clean up the zip file
    echo "🗑️   Removing dist.zip..."
    rm -f dist.zip
    echo "✅ dist.zip cleaned up"

    # Step 6: Restart kiwi-ui docker container
    echo "🔄 Restarting kiwi-ui docker container..."
    if docker ps --format "table {{.Names}}" | grep -q "kiwi-ui"; then
        docker restart kiwi-ui
        echo "✅ kiwi-ui container restarted successfully"
    else
        echo "⚠️  Warning: kiwi-ui container not found or not running"
        echo "   Available containers:"
        docker ps --format "table {{.Names}}\t{{.Status}}"
        echo ""
        echo "   You may need to start it manually with: docker start kiwi-ui"
    fi

    echo ""
    echo "🎉 UI deployment completed successfully!"
    echo "   Your new UI files are now active in the kiwi-ui container."
}

# Main execution logic based on mode
case $MODE in
    "kp")
        echo "🎯 Mode: Kill Process on Port"
        kill_process_on_port
        ;;
    "d")
        echo "🚀 Mode: Deploy UI"
        deploy_ui
        ;;
    *)
        echo "❌ Error: Invalid mode '$MODE'"
        echo "Available modes:"
        echo "  -mode=d   : Deploy UI (default)"
        echo "  -mode=kp  : Kill process on port (requires -port=<number>)"
        echo ""
        echo "Usage examples:"
        echo "  $0                    # Deploy UI (default)"
        echo "  $0 -mode=d           # Deploy UI explicitly"
        echo "  $0 -mode=kp -port=3000  # Kill process on port 3000"
        exit 1
        ;;
esac