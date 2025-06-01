#!/bin/bash

# UI Deployment Script for Kiwi Docker Container (macOS/Linux Compatible)
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

# Function to get the real user's home directory (cross-platform)
get_user_home() {
    # Detect OS
    OS=$(uname -s)

    if [ "$OS" = "Darwin" ]; then
        # macOS
        if [ -n "$SUDO_USER" ]; then
            # Running with sudo, get the real user's home
            USER_HOME=$(eval echo "~$SUDO_USER")
        else
            # Not running with sudo
            USER_HOME="$HOME"
        fi
    else
        # Linux
        if [ -n "$SUDO_USER" ]; then
            # Running with sudo, get the real user's home
            USER_HOME=$(getent passwd $SUDO_USER | cut -d: -f6 2>/dev/null || eval echo "~$SUDO_USER")
        else
            # Not running with sudo
            USER_HOME="$HOME"
        fi
    fi

    echo "$USER_HOME"
}

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
    USER_HOME=$(get_user_home)
    echo "USER_HOME is: $USER_HOME"

    # Verify the home directory exists and is accessible
    if [ ! -d "$USER_HOME" ]; then
        echo "❌ Error: User home directory '$USER_HOME' not found or not accessible"
        exit 1
    fi

    # Step 1: Clean up existing UI dist directory first
    echo "🧹 Cleaning up existing UI distribution files..."
    if [ -d "$USER_HOME/docker/ui/dist" ]; then
        rm -rf "$USER_HOME/docker/ui/dist"
        echo "✅ Old dist directory removed"
    else
        echo "ℹ️  No existing dist directory found"
    fi

    # Step 2: Check for dist.zip in multiple locations
    echo "📦 Looking for dist.zip file..."
    DIST_ZIP_PATH=""

    # Check common locations for dist.zip
    POSSIBLE_PATHS=(
        "$USER_HOME/dist.zip"
        "$USER_HOME/Downloads/dist.zip"
        "$USER_HOME/Desktop/dist.zip"
        "$(pwd)/dist.zip"
    )

    for path in "${POSSIBLE_PATHS[@]}"; do
        if [ -f "$path" ]; then
            DIST_ZIP_PATH="$path"
            echo "✅ Found dist.zip at: $DIST_ZIP_PATH"
            break
        fi
    done

    if [ -z "$DIST_ZIP_PATH" ]; then
        echo "❌ Error: dist.zip not found in any of these locations:"
        for path in "${POSSIBLE_PATHS[@]}"; do
            echo "   - $path"
        done
        echo ""
        echo "Please place dist.zip in one of the above locations or in the current directory."
        exit 1
    fi

    # Step 3: Ensure docker/ui directory exists
    echo "📂 Ensuring docker/ui directory exists..."
    mkdir -p "$USER_HOME/docker/ui/"

    # Step 4: Copy dist.zip to docker directory
    echo "📦 Copying dist.zip to docker directory..."
    cp "$DIST_ZIP_PATH" "$USER_HOME/docker/ui/"
    echo "✅ dist.zip copied successfully"

    # Step 5: Navigate to docker/ui directory and extract
    echo "📂 Navigating to docker/ui directory..."
    cd "$USER_HOME/docker/ui/"

    echo "📤 Extracting dist.zip..."
    if [ ! -f dist.zip ]; then
        echo "❌ Error: dist.zip not found in $USER_HOME/docker/ui/"
        exit 1
    fi

    # Extract with error handling
    if unzip -q dist.zip; then
        echo "✅ Files extracted successfully"
    else
        echo "❌ Error: Failed to extract dist.zip"
        exit 1
    fi

    # Step 6: Clean up macOS metadata files
    echo "🧹 Removing macOS metadata files..."
    if [ -d __MACOSX/ ]; then
        rm -rf __MACOSX/
        echo "✅ __MACOSX directory removed"
    else
        echo "ℹ️  No __MACOSX directory found"
    fi

    # Step 7: Clean up the zip file
    echo "🗑️  Removing dist.zip..."
    rm -f dist.zip
    echo "✅ dist.zip cleaned up"

    # Step 8: Verify Docker is available
    echo "🐳 Checking Docker availability..."
    if ! command -v docker &> /dev/null; then
        echo "❌ Error: Docker command not found"
        echo "Please ensure Docker Desktop is installed and running"
        exit 1
    fi

    # Step 9: Restart kiwi-ui docker container
    echo "🔄 Restarting kiwi-ui docker container..."
    if docker ps --format "table {{.Names}}" | grep -q "kiwi-ui"; then
        echo "🔄 Container found, restarting..."
        docker restart kiwi-ui
        echo "✅ kiwi-ui container restarted successfully"

        # Wait a moment and check if container is running
        sleep 3
        if docker ps --format "table {{.Names}}" | grep -q "kiwi-ui"; then
            echo "✅ Container is running successfully"
        else
            echo "⚠️  Warning: Container may not have started properly"
        fi
    else
        echo "⚠️  Warning: kiwi-ui container not found or not running"
        echo "   Available containers:"
        docker ps --format "table {{.Names}}\t{{.Status}}" 2>/dev/null || echo "   No containers running"
        echo ""
        echo "   Checking if container exists but is stopped..."
        if docker ps -a --format "table {{.Names}}" | grep -q "kiwi-ui"; then
            echo "   Container exists but is stopped. Starting it..."
            docker start kiwi-ui
            echo "✅ kiwi-ui container started"
        else
            echo "   Container does not exist. You may need to run the setup script first."
        fi
    fi

    echo ""
    echo "🎉 UI deployment completed successfully!"
    echo "   Your new UI files are now active in the kiwi-ui container."
    echo "   You can access the UI at: http://localhost:80"
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