#!/bin/bash

# UI Deployment Script for Kiwi Docker Container
# This script updates the UI files and restarts the kiwi-ui container

set -e  # Exit on any error

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