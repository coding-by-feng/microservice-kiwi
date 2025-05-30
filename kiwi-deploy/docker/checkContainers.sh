#!/bin/bash

echo "=== KIWI CONTAINERS DEBUG MENU ==="

# Define container names and their corresponding docker container names
declare -A containers=(
    ["1"]="kiwi-base-kiwi-eureka-1|EUREKA"
    ["2"]="kiwi-base-kiwi-config-1|CONFIG"
    ["3"]="kiwi-service-kiwi-upms-1|UPMS"
    ["4"]="kiwi-service-kiwi-auth-1|AUTH"
    ["5"]="kiwi-service-kiwi-gate-1|GATE"
    ["6"]="kiwi-service-kiwi-word-biz-1|WORD-BIZ"
    ["7"]="kiwi-service-kiwi-ai-biz-1|AI-BIZ"
    ["8"]="kiwi-service-kiwi-crawler-1|CRAWLER"
    ["9"]="kiwi-mysql|MYSQL"
    ["10"]="kiwi-redis|REDIS"
    ["11"]="kiwi-rabbit|RABBITMQ"
    ["12"]="kiwi-ui|KIWI-UI"
    ["13"]="kiwi-es|ELASTICSEARCH"
    ["14"]="storage|FASTDFS-STORAGE"
    ["15"]="tracker|FASTDFS-TRACKER"
)

show_menu() {
    echo ""
    echo "Choose which container logs to check:"
    echo "1) EUREKA (kiwi-base-kiwi-eureka-1)"
    echo "2) CONFIG (kiwi-base-kiwi-config-1)"
    echo "3) UPMS (kiwi-service-kiwi-upms-1)"
    echo "4) AUTH (kiwi-service-kiwi-auth-1)"
    echo "5) GATE (kiwi-service-kiwi-gate-1)"
    echo "6) WORD-BIZ (kiwi-service-kiwi-word-biz-1)"
    echo "7) AI-BIZ (kiwi-service-kiwi-ai-biz-1)"
    echo "8) CRAWLER (kiwi-service-kiwi-crawler-1)"
    echo "9) MYSQL (kiwi-mysql)"
    echo "10) REDIS (kiwi-redis)"
    echo "11) RABBITMQ (kiwi-rabbit)"
    echo "12) KIWI-UI (kiwi-ui)"
    echo "13) ELASTICSEARCH (kiwi-es)"
    echo "14) FASTDFS-STORAGE (storage)"
    echo "15) FASTDFS-TRACKER (tracker)"
    echo ""
    echo "Additional options:"
    echo "16) Check all failed containers"
    echo "17) Check connectivity tests"
    echo "18) Show resource usage"
    echo "19) Show container status"
    echo "0) Exit"
    echo ""
}

check_logs() {
    local container_name=$1
    local service_name=$2
    local lines=${3:-50}

    echo -e "\n=== $service_name LOGS (last $lines lines) ==="
    if sudo docker logs "$container_name" --tail "$lines" 2>/dev/null; then
        echo "✓ Logs retrieved successfully"
    else
        echo "✗ Failed to retrieve logs for $container_name"
        echo "Container may not exist or may be stopped"
    fi
}

check_connectivity() {
    echo -e "\n=== CHECKING NETWORK CONNECTIVITY ==="

    echo "Testing database connectivity..."
    if sudo docker exec kiwi-mysql mysqladmin ping -h localhost 2>/dev/null; then
        echo "✓ MySQL is responsive"
    else
        echo "✗ MySQL connection failed"
    fi

    echo -e "\nTesting Redis connectivity..."
    if sudo docker exec kiwi-redis redis-cli ping 2>/dev/null; then
        echo "✓ Redis is responsive"
    else
        echo "✗ Redis connection failed"
    fi

    echo -e "\nTesting RabbitMQ..."
    if sudo docker exec kiwi-rabbit rabbitmqctl status 2>/dev/null; then
        echo "✓ RabbitMQ is running"
    else
        echo "✗ RabbitMQ status check failed"
    fi

    echo -e "\nTesting Elasticsearch..."
    if curl -s http://localhost:9200/_cluster/health >/dev/null 2>&1; then
        echo "✓ Elasticsearch is responsive"
    else
        echo "✗ Elasticsearch connection failed"
    fi

    echo -e "\nTesting FastDFS Tracker..."
    if sudo docker exec tracker ps aux | grep fdfs_trackerd >/dev/null 2>&1; then
        echo "✓ FastDFS Tracker is running"
    else
        echo "✗ FastDFS Tracker check failed"
    fi

    echo -e "\nTesting FastDFS Storage..."
    if sudo docker exec storage ps aux | grep fdfs_storaged >/dev/null 2>&1; then
        echo "✓ FastDFS Storage is running"
    else
        echo "✗ FastDFS Storage check failed"
    fi
}

show_resource_usage() {
    echo -e "\n=== CONTAINER RESOURCE USAGE ==="
    sudo docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"
}

show_container_status() {
    echo -e "\n=== CONTAINER STATUS ==="
    sudo docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep kiwi
}

check_all_failed() {
    echo -e "\n=== CHECKING ALL KIWI SERVICE CONTAINERS ==="
    for key in {1..8}; do
        IFS='|' read -r container_name service_name <<< "${containers[$key]}"
        check_logs "$container_name" "$service_name" 30
        echo ""
    done
}

# Main loop
while true; do
    show_menu
    read -p "Enter your choice (0-15): " choice

    case $choice in
        0)
            echo "Exiting..."
            exit 0
            ;;
        1|2|3|4|5|6|7|8|9|10|11|12|13|14|15)
            IFS='|' read -r container_name service_name <<< "${containers[$choice]}"
            read -p "How many log lines to show? (default: 50): " lines
            lines=${lines:-50}
            check_logs "$container_name" "$service_name" "$lines"
            ;;
        16)
            check_all_failed
            ;;
        17)
            check_connectivity
            ;;
        18)
            show_resource_usage
            ;;
        19)
            show_container_status
            ;;
        *)
            echo "Invalid choice. Please enter a number between 0-19."
            ;;
    esac

    echo ""
    read -p "Press Enter to continue..."
done