#!/bin/bash

echo "=== DEBUGGING FAILED KIWI CONTAINERS ==="

# Check logs for each failed service
echo "Checking logs for failed containers..."

echo -e "\n=== EUREKA LOGS ==="
sudo docker logs kiwi-base-kiwi-eureka-1 --tail 50

echo -e "\n=== CONFIG LOGS ==="
sudo docker logs kiwi-base-kiwi-config-1 --tail 50

echo -e "\n=== UPMS LOGS ==="
sudo docker logs kiwi-service-kiwi-upms-1 --tail 50

echo -e "\n=== AUTH LOGS ==="
sudo docker logs kiwi-service-kiwi-auth-1 --tail 50

echo -e "\n=== GATE LOGS ==="
sudo docker logs kiwi-service-kiwi-gate-1 --tail 50

echo -e "\n=== WORD-BIZ LOGS ==="
sudo docker logs kiwi-service-kiwi-word-biz-1 --tail 50

echo -e "\n=== AI-BIZ LOGS ==="
sudo docker logs kiwi-service-kiwi-ai-biz-1 --tail 50

echo -e "\n=== CRAWLER LOGS ==="
sudo docker logs kiwi-service-kiwi-crawler-1 --tail 50

echo -e "\n=== CHECKING NETWORK CONNECTIVITY ==="
# Check if services can reach dependencies
echo "Testing database connectivity..."
sudo docker exec kiwi-mysql mysqladmin ping -h localhost

echo -e "\nTesting Redis connectivity..."
sudo docker exec kiwi-redis redis-cli ping

echo -e "\nTesting RabbitMQ..."
sudo docker exec kiwi-rabbit rabbitmqctl status

echo -e "\n=== CONTAINER RESOURCE USAGE ==="
sudo docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"