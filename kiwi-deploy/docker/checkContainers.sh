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
    echo "20) Check and start stopped containers"
    echo "21) Start all stopped Kiwi containers"
    echo "0) Exit"
    echo ""
}

check_container_status() {
    local container_name=$1
    local service_name=$2

    if sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
        echo "✓ $service_name ($container_name) is RUNNING"
        return 0
    elif sudo docker ps -a --format "{{.Names}}" | grep -q "^${container_name}$"; then
        echo "✗ $service_name ($container_name) is STOPPED"
        return 1
    else
        echo "✗ $service_name ($container_name) does NOT EXIST"
        return 2
    fi
}

start_container() {
    local container_name=$1
    local service_name=$2

    echo "Starting $service_name ($container_name)..."
    if sudo docker start "$container_name" >/dev/null 2>&1; then
        echo "✓ $service_name started successfully"
        # Wait a moment and check if it's still running
        sleep 2
        if sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
            echo "✓ $service_name is running stable"
        else
            echo "⚠ $service_name started but may have issues - check logs"
        fi
    else
        echo "✗ Failed to start $service_name"
    fi
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

check_and_start_containers() {
    echo -e "\n=== CHECKING ALL CONTAINER STATUS ==="
    local stopped_containers=()

    for key in {1..15}; do
        IFS='|' read -r container_name service_name <<< "${containers[$key]}"
        if ! check_container_status "$container_name" "$service_name"; then
            if sudo docker ps -a --format "{{.Names}}" | grep -q "^${container_name}$"; then
                stopped_containers+=("$key|$container_name|$service_name")
            fi
        fi
    done

    if [ ${#stopped_containers[@]} -eq 0 ]; then
        echo -e "\n✓ All containers are running!"
        return
    fi

    echo -e "\nFound ${#stopped_containers[@]} stopped container(s):"
    for item in "${stopped_containers[@]}"; do
        IFS='|' read -r key container_name service_name <<< "$item"
        echo "  - $service_name ($container_name)"
    done

    echo ""
    read -p "Do you want to start all stopped containers? (y/N): " choice
    if [[ $choice == "y" || $choice == "Y" ]]; then
        echo -e "\nStarting stopped containers..."
        for item in "${stopped_containers[@]}"; do
            IFS='|' read -r key container_name service_name <<< "$item"
            start_container "$container_name" "$service_name"
            echo ""
        done
    else
        echo "Skipping container startup."
    fi
}

start_all_kiwi_containers() {
    echo -e "\n=== STARTING ALL KIWI CONTAINERS ==="
    echo "This will attempt to start all Kiwi containers in dependency order..."
    echo ""
    read -p "Continue? (y/N): " choice
    if [[ ! ($choice == "y" || $choice == "Y") ]]; then
        echo "Cancelled."
        return
    fi

    # Start in dependency order
    local startup_order=(
        "9|kiwi-mysql|MYSQL"
        "10|kiwi-redis|REDIS"
        "11|kiwi-rabbit|RABBITMQ"
        "13|kiwi-es|ELASTICSEARCH"
        "14|storage|FASTDFS-STORAGE"
        "15|tracker|FASTDFS-TRACKER"
        "1|kiwi-base-kiwi-eureka-1|EUREKA"
        "2|kiwi-base-kiwi-config-1|CONFIG"
        "3|kiwi-service-kiwi-upms-1|UPMS"
        "4|kiwi-service-kiwi-auth-1|AUTH"
        "5|kiwi-service-kiwi-gate-1|GATE"
        "6|kiwi-service-kiwi-word-biz-1|WORD-BIZ"
        "7|kiwi-service-kiwi-ai-biz-1|AI-BIZ"
        "8|kiwi-service-kiwi-crawler-1|CRAWLER"
        "12|kiwi-ui|KIWI-UI"
    )

    for item in "${startup_order[@]}"; do
        IFS='|' read -r key container_name service_name <<< "$item"

        if check_container_status "$container_name" "$service_name" >/dev/null 2>&1; then
            echo "✓ $service_name is already running"
        else
            start_container "$container_name" "$service_name"
            # Add delay between services to allow proper startup
            if [[ $key -le 6 ]]; then  # Infrastructure services need more time
                echo "  Waiting 5 seconds for $service_name to initialize..."
                sleep 5
            else
                sleep 2
            fi
        fi
        echo ""
    done

    echo "=== STARTUP COMPLETE ==="
    echo "Checking final status..."
    show_container_status
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
    printf "%-30s %-15s %-20s\n" "CONTAINER NAME" "STATUS" "PORTS"
    printf "%-30s %-15s %-20s\n" "------------------------------" "---------------" "--------------------"

    for key in {1..15}; do
        IFS='|' read -r container_name service_name <<< "${containers[$key]}"
        if sudo docker ps --format "{{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -q "^${container_name}"; then
            status="RUNNING"
            ports=$(sudo docker ps --format "{{.Ports}}" --filter "name=${container_name}")
        elif sudo docker ps -a --format "{{.Names}}" | grep -q "^${container_name}$"; then
            status="STOPPED"
            ports="-"
        else
            status="NOT EXISTS"
            ports="-"
        fi
        printf "%-30s %-15s %-20s\n" "$container_name" "$status" "$ports"
    done
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
    read -p "Enter your choice (0-21): " choice

    case $choice in
        0)
            echo "Exiting..."
            exit 0
            ;;
        1|2|3|4|5|6|7|8|9|10|11|12|13|14|15)
            IFS='|' read -r container_name service_name <<< "${containers[$choice]}"

            # First check if container is running
            echo -e "\n=== CHECKING $service_name STATUS ==="
            status_code=0
            check_container_status "$container_name" "$service_name" || status_code=$?

            if [ $status_code -eq 1 ]; then  # Container exists but is stopped
                echo ""
                read -p "Container is stopped. Do you want to start it? (y/N): " start_choice
                if [[ $start_choice == "y" || $start_choice == "Y" ]]; then
                    start_container "$container_name" "$service_name"
                fi
            elif [ $status_code -eq 2 ]; then  # Container doesn't exist
                echo "Cannot show logs - container does not exist."
                echo ""
                read -p "Press Enter to continue..."
                continue
            fi

            # Show logs if container is running or user chose not to start
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
        20)
            check_and_start_containers
            ;;
        21)
            start_all_kiwi_containers
            ;;
        *)
            echo "Invalid choice. Please enter a number between 0-21."
            ;;
    esac

    echo ""
    read -p "Press Enter to continue..."
done