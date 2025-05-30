#!/bin/bash

# Stop autoCheckService if running
if pgrep -f "autoCheckService.sh" >/dev/null; then
  echo "Stopping autoCheckService..."
  pkill -f "autoCheckService.sh"
  echo "✅ AutoCheckService stopped"
else
  echo "ℹ️  AutoCheckService is not running"
fi

echo "=============================================="
echo "STOPPING CONTAINERS..."
echo "=============================================="

# Define the desired stop order
desired_order=("kiwi-crawler" "kiwi-ai-biz" "kiwi-ai-biz-batch" "kiwi-word-biz" "kiwi-upms" "kiwi-auth" "kiwi-gate" "kiwi-config" "kiwi-eureka")

# Stop containers in the specified order
for name in "${desired_order[@]}"; do
    container_id=$(docker ps -a --filter "name=$name" --format "{{.ID}}")
    if [ -n "$container_id" ]; then
        echo "Stopping container: $name (ID: $container_id)"
        # Use --timeout instead of deprecated --time flag
        docker stop --timeout=30 "$container_id"
        echo "✅ Stopped: $name"
    else
        echo "ℹ️  Container not found: $name"
    fi
done

echo "✅ All containers stopped successfully"

echo "=============================================="
echo "REMOVING CONTAINERS..."
echo "=============================================="

# Force remove containers if they exist
for name in "${desired_order[@]}"; do
    container_id=$(docker ps -a --filter "name=$name" --format "{{.ID}}")
    if [ -n "$container_id" ]; then
        echo "Removing container: $name (ID: $container_id)"
        docker rm -f "$container_id"
        echo "✅ Removed: $name"
    else
        echo "ℹ️  Container not found for removal: $name"
    fi
done

echo "=============================================="
echo "🎉 CLEANUP COMPLETED SUCCESSFULLY!"
echo "=============================================="

# Optional: Clean up dangling images and unused networks
read -p "Do you want to clean up dangling images and unused networks? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Cleaning up dangling images..."
    docker image prune -f
    echo "Cleaning up unused networks..."
    docker network prune -f
    echo "✅ Docker cleanup completed"
fi