#!/bin/bash

# Stop autoCheckService if running
if pgrep -f "autoCheckService.sh" >/dev/null; then
  echo "Stopping autoCheckService..."
  pkill -f "autoCheckService.sh"
fi

echo "Stopping containers..."

# Define the desired stop order
desired_order=("kiwi-crawler" "kiwi-ai-biz" "kiwi-ai-batch" "kiwi-word-biz" "kiwi-upms" "kiwi-auth" "kiwi-gate" "kiwi-config" "kiwi-eureka")

# Stop containers in the specified order
for name in "${desired_order[@]}"; do
    container_id=$(podman ps -a --filter "name=$name" --format "{{.ID}}")
    if [ -n "$container_id" ]; then
        echo "Stopping container: $name"
        podman stop --time=30 "$container_id"
    fi
done

echo "Containers stopped successfully"

echo "delete container beginning"

# Force remove containers if they exist
for name in "${desired_order[@]}"; do
    container_id=$(podman ps -a --filter "name=$name" --format "{{.ID}}")
    if [ -n "$container_id" ]; then
        echo "Removing container: $name with ID: $container_id"
        podman rm -f "$container_id"
        echo "Removed"
    fi
done

