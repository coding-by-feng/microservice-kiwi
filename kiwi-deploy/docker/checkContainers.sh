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
    ["9"]="kiwi-service-kiwi-tools-biz-1|TOOLS-BIZ"             # NEW
    ["10"]="kiwi-mysql|MYSQL"
    ["11"]="kiwi-redis|REDIS"
    ["12"]="kiwi-rabbit|RABBITMQ"
    ["13"]="kiwi-ui|KIWI-UI"
    ["14"]="kiwi-es|ELASTICSEARCH"
    ["15"]="storage|FASTDFS-STORAGE"
    ["16"]="tracker|FASTDFS-TRACKER"
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
    echo "9) TOOLS-BIZ (kiwi-service-kiwi-tools-biz-1)"          # NEW
    echo "10) MYSQL (kiwi-mysql)"
    echo "11) REDIS (kiwi-redis)"
    echo "12) RABBITMQ (kiwi-rabbit)"
    echo "13) KIWI-UI (kiwi-ui)"
    echo "14) ELASTICSEARCH (kiwi-es)"
    echo "15) FASTDFS-STORAGE (storage)"
    echo "16) FASTDFS-TRACKER (tracker)"
    echo ""
    echo "Additional options:"
    echo "17) Check all failed containers"
    echo "18) Check connectivity tests"
    echo "19) Show resource usage"
    echo "20) Show container status"
    echo "21) Check and start stopped containers"
    echo "22) Start all stopped Kiwi containers"
    echo "23) Start infrastructure containers only"
    echo "24) Enter container shell"
    echo "25) Stop one container"
    echo "26) Restart one container"
    echo "27) Live multi-pane logs dashboard (tail -f all)"  # UPDATED
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

stop_container() {
    local container_name=$1
    local service_name=$2
    local graceful_timeout=${3:-10}

    echo "Stopping $service_name ($container_name)..."

    # Check if container is actually running
    if ! sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
        echo "✗ $service_name is not currently running"
        return 1
    fi

    # Attempt graceful shutdown first
    if sudo docker stop --time="$graceful_timeout" "$container_name" >/dev/null 2>&1; then
        echo "✓ $service_name stopped successfully"

        # Verify it's actually stopped
        sleep 1
        if sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
            echo "⚠ $service_name may still be running - forcing stop..."
            if sudo docker kill "$container_name" >/dev/null 2>&1; then
                echo "✓ $service_name force-stopped"
            else
                echo "✗ Failed to force-stop $service_name"
                return 1
            fi
        else
            echo "✓ $service_name shutdown confirmed"
        fi
    else
        echo "✗ Failed to stop $service_name gracefully, attempting force stop..."
        if sudo docker kill "$container_name" >/dev/null 2>&1; then
            echo "✓ $service_name force-stopped"
        else
            echo "✗ Failed to force-stop $service_name"
            return 1
        fi
    fi

    return 0
}

restart_container() {
    local container_name=$1
    local service_name=$2
    local graceful_timeout=${3:-10}

    echo "=== RESTARTING $service_name ($container_name) ==="

    # Check if container exists
    if ! sudo docker ps -a --format "{{.Names}}" | grep -q "^${container_name}$"; then
        echo "✗ Container $service_name does not exist"
        return 1
    fi

    # Check current status
    local is_running=false
    if sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
        is_running=true
        echo "Container is currently running - will stop first"
    else
        echo "Container is stopped - will start directly"
    fi

    # If running, stop it first
    if [ "$is_running" = true ]; then
        echo ""
        echo "Step 1: Stopping container..."
        if stop_container "$container_name" "$service_name" "$graceful_timeout"; then
            echo "✓ Container stopped successfully"
        else
            echo "✗ Failed to stop container"
            return 1
        fi

        # Wait a moment to ensure clean shutdown
        echo "Waiting 3 seconds for clean shutdown..."
        sleep 3
    fi

    # Start the container
    echo ""
    echo "Step 2: Starting container..."

    # Special handling for kiwi-ui (port 80 check)
    if [ "$container_name" == "kiwi-ui" ]; then
        if check_port_80_and_handle; then
            start_container "$container_name" "$service_name"
        else
            echo "✗ Cannot start $service_name due to port 80 conflict"
            return 1
        fi
    else
        start_container "$container_name" "$service_name"
    fi

    echo ""
    echo "=== RESTART COMPLETE ==="

    # Show final status
    echo "Final status:"
    check_container_status "$container_name" "$service_name"

    return 0
}

restart_single_container() {
    echo -e "\n=== RESTART CONTAINER ==="
    echo "Choose which container to restart:"
    echo ""

    # Show all containers with their current status
    for key in {1..16}; do   # UPDATED (was 1..15)
        IFS='|' read -r container_name service_name <<< "${containers[$key]}"
        if sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
            status="RUNNING"
        elif sudo docker ps -a --format "{{.Names}}" | grep -q "^${container_name}$"; then
            status="STOPPED"
        else
            status="NOT EXISTS"
        fi

        echo "$key) $service_name ($container_name) - $status"
    done

    if [ ${#containers[@]} -eq 0 ]; then
        echo "No containers are currently running."
        return
    fi

    echo ""
    read -p "Enter container number to restart (1-16): " container_choice   # UPDATED
    if [[ ! $container_choice =~ ^([1-9]|1[0-6])$ ]]; then                  # UPDATED
        echo "Invalid choice. Please enter a number between 1-16."
        return
    fi

    IFS='|' read -r container_name service_name <<< "${containers[$container_choice]}"

    # Check if container exists
    if ! sudo docker ps -a --format "{{.Names}}" | grep -q "^${container_name}$"; then
        echo "✗ Container $service_name does not exist. Cannot restart."
        return
    fi

    echo ""
    echo "You selected: $service_name ($container_name)"

    # Warning for critical infrastructure containers
    case $container_choice in
        9|10|11|13)  # MySQL, Redis, RabbitMQ, Elasticsearch
            echo "⚠ WARNING: This is a critical infrastructure container!"
            echo "  Restarting $service_name may temporarily disrupt application services."
            ;;
        1|2)  # Eureka, Config
            echo "⚠ WARNING: This is a core service container!"
            echo "  Restarting $service_name may affect service discovery and configuration."
            ;;
        12)  # Kiwi UI
            echo "ℹ Note: Restarting KIWI-UI will temporarily make the web interface unavailable."
            ;;
    esac

    echo ""
    read -p "Are you sure you want to restart $service_name? (y/N): " confirm
    if [[ ! ($confirm == "y" || $confirm == "Y") ]]; then
        echo "Operation cancelled."
        return
    fi

    # Ask for graceful shutdown timeout (only if container is running)
    if sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
        echo ""
        read -p "Graceful shutdown timeout in seconds (default: 10): " timeout
        timeout=${timeout:-10}

        # Validate timeout is a number
        if ! [[ "$timeout" =~ ^[0-9]+$ ]]; then
            echo "Invalid timeout. Using default 10 seconds."
            timeout=10
        fi
    else
        timeout=10  # Default for stopped containers
    fi

    echo ""
    restart_container "$container_name" "$service_name" "$timeout"
}

stop_single_container() {
    echo -e "\n=== STOP CONTAINER ==="
    echo "Choose which container to stop:"
    echo ""

    # Show current status and only display running containers
    local running_containers=()
    for key in {1..16}; do   # UPDATED (was 1..15)
        IFS='|' read -r container_name service_name <<< "${containers[$key]}"
        if sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
            running_containers+=("$key")
            echo "$key) $service_name ($container_name) - RUNNING"
        fi
    done

    if [ ${#running_containers[@]} -eq 0 ]; then
        echo "No containers are currently running."
        return
    fi

    echo ""
    read -p "Enter container number to stop: " container_choice

    # Validate choice
    local valid_choice=false
    for valid_key in "${running_containers[@]}"; do
        if [ "$container_choice" == "$valid_key" ]; then
            valid_choice=true
            break
        fi
    done

    if [ "$valid_choice" = false ]; then
        echo "Invalid choice or container is not running."
        return
    fi

    IFS='|' read -r container_name service_name <<< "${containers[$container_choice]}"

    echo ""
    echo "You selected: $service_name ($container_name)"

    # Warning for critical infrastructure containers
    case $container_choice in
        9|10|11|13)  # MySQL, Redis, RabbitMQ, Elasticsearch
            echo "⚠ WARNING: This is a critical infrastructure container!"
            echo "  Stopping $service_name may cause application services to fail."
            ;;
        12)  # Kiwi UI
            echo "ℹ Note: Stopping KIWI-UI will make the web interface unavailable."
            ;;
    esac

    echo ""
    read -p "Are you sure you want to stop $service_name? (y/N): " confirm
    if [[ ! ($confirm == "y" || $confirm == "Y") ]]; then
        echo "Operation cancelled."
        return
    fi

    # Ask for graceful shutdown timeout
    echo ""
    read -p "Graceful shutdown timeout in seconds (default: 10): " timeout
    timeout=${timeout:-10}

    # Validate timeout is a number
    if ! [[ "$timeout" =~ ^[0-9]+$ ]]; then
        echo "Invalid timeout. Using default 10 seconds."
        timeout=10
    fi

    echo ""
    stop_container "$container_name" "$service_name" "$timeout"

    echo ""
    echo "=== STOP OPERATION COMPLETE ==="

    # Show updated status
    echo "Current status:"
    check_container_status "$container_name" "$service_name"
}

check_port_80_and_handle() {
    echo "Checking if port 80 is in use..."

    # Check if port 80 is being used
    local port_info=$(sudo netstat -tulpn | grep ":80 ")

    if [ -n "$port_info" ]; then
        echo "⚠ Port 80 is currently in use:"
        echo "$port_info"
        echo ""

        # Extract PID from netstat output
        local pid=$(echo "$port_info" | awk '{print $7}' | cut -d'/' -f1 | head -n1)

        if [ -n "$pid" ] && [ "$pid" != "-" ]; then
            # Get process details
            local process_info=$(ps -p "$pid" -o pid,ppid,cmd --no-headers 2>/dev/null)
            if [ -n "$process_info" ]; then
                echo "Process details:"
                echo "PID: $pid"
                echo "Command: $(ps -p "$pid" -o cmd --no-headers 2>/dev/null)"
                echo ""

                read -p "Do you want to kill this process to free port 80? (y/N): " kill_choice
                if [[ $kill_choice == "y" || $kill_choice == "Y" ]]; then
                    echo "Attempting to terminate process $pid..."

                    # Try graceful termination first
                    if sudo kill "$pid" 2>/dev/null; then
                        echo "Sent TERM signal to process $pid"
                        sleep 3

                        # Check if process is still running
                        if kill -0 "$pid" 2>/dev/null; then
                            echo "Process still running, forcing termination..."
                            sudo kill -9 "$pid" 2>/dev/null
                            sleep 2
                        fi

                        # Verify port is now free
                        if sudo netstat -tulpn | grep -q ":80 "; then
                            echo "⚠ Port 80 is still in use after killing process"
                            return 1
                        else
                            echo "✓ Port 80 is now free"
                            return 0
                        fi
                    else
                        echo "✗ Failed to kill process $pid"
                        return 1
                    fi
                else
                    echo "Skipping process termination. kiwi-ui may fail to start."
                    return 1
                fi
            else
                echo "Could not get process details for PID $pid"
                return 1
            fi
        else
            echo "Could not determine PID using port 80"
            return 1
        fi
    else
        echo "✓ Port 80 is available"
        return 0
    fi
}

start_infrastructure_containers() {
    echo -e "\n=== STARTING INFRASTRUCTURE CONTAINERS ==="
    echo "This will start the following containers in order:"
    echo "  - MySQL"
    echo "  - Redis"
    echo "  - RabbitMQ"
    echo "  - Elasticsearch"
    echo "  - FastDFS Storage"
    echo "  - FastDFS Tracker"
    echo "  - Kiwi UI (with port 80 check)"
    echo ""
    echo "NOTE: Each container will wait 30 seconds after starting to ensure proper initialization."
    echo ""
    read -p "Continue? (y/N): " choice
    if [[ ! ($choice == "y" || $choice == "Y") ]]; then
        echo "Cancelled."
        return
    fi

    # Infrastructure containers in dependency order
    local infra_order=(
        "10|kiwi-mysql|MYSQL"
        "11|kiwi-redis|REDIS"
        "12|kiwi-rabbit|RABBITMQ"
        "14|kiwi-es|ELASTICSEARCH"
        "15|storage|FASTDFS-STORAGE"
        "16|tracker|FASTDFS-TRACKER"
    )

    echo -e "\n=== STARTING INFRASTRUCTURE SERVICES ==="
    local total_containers=${#infra_order[@]}
    local current_container=0
    
    for item in "${infra_order[@]}"; do
        IFS='|' read -r key container_name service_name <<< "$item"
        ((current_container++))
        
        echo "[$current_container/$total_containers] Processing $service_name..."

        if check_container_status "$container_name" "$service_name" >/dev/null 2>&1; then
            echo "✓ $service_name is already running"
        else
            start_container "$container_name" "$service_name"
            if [ $current_container -lt $total_containers ]; then
                echo "  Waiting 30 seconds for $service_name to fully initialize before starting next container..."
                # Show countdown for better user experience
                for i in {30..1}; do
                    printf "\r  Countdown: %02d seconds remaining..." $i
                    sleep 1
                done
                printf "\r  ✓ Wait complete. Ready for next container.                    \n"
            else
                echo "  Final container started. Waiting 10 seconds for stabilization..."
                sleep 10
            fi
        fi
        echo ""
    done

    # Handle Kiwi UI with port check
    echo "=== STARTING KIWI UI ==="
    if check_container_status "kiwi-ui" "KIWI-UI" >/dev/null 2>&1; then
        echo "✓ KIWI-UI is already running"
    else
        if check_port_80_and_handle; then
            start_container "kiwi-ui" "KIWI-UI"
            sleep 3
        else
            echo "⚠ Skipping KIWI-UI startup due to port 80 conflict"
        fi
    fi
    echo ""

    echo -e "\n=== INFRASTRUCTURE STARTUP COMPLETE ==="
    echo "Checking status of infrastructure containers..."

    # Show status of infrastructure containers
    printf "%-30s %-15s\n" "CONTAINER NAME" "STATUS"
    printf "%-30s %-15s\n" "------------------------------" "---------------"

    for item in "${infra_order[@]}"; do
        IFS='|' read -r key container_name service_name <<< "$item"
        if sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
            status="RUNNING"
        elif sudo docker ps -a --format "{{.Names}}" | grep -q "^${container_name}$"; then
            status="STOPPED"
        else
            status="NOT EXISTS"
        fi
        printf "%-30s %-15s\n" "$container_name" "$status"
    done

    # Check kiwi-ui status
    if sudo docker ps --format "{{.Names}}" | grep -q "^kiwi-ui$"; then
        status="RUNNING"
    elif sudo docker ps -a --format "{{.Names}}" | grep -q "^kiwi-ui$"; then
        status="STOPPED"
    else
        status="NOT EXISTS"
    fi
    printf "%-30s %-15s\n" "kiwi-ui" "$status"
}

enter_container() {
    echo -e "\n=== ENTER CONTAINER SHELL ==="
    echo "Choose which container to enter:"
    echo ""
    for key in {1..16}; do   # UPDATED
        IFS='|' read -r container_name service_name <<< "${containers[$key]}"
        echo "$key) $service_name ($container_name)"
    done
    echo ""
    read -p "Enter container number (1-16): " container_choice  # UPDATED

    if [[ ! $container_choice =~ ^([1-9]|1[0-6])$ ]]; then      # UPDATED
        echo "Invalid choice. Please enter a number between 1-16."
        return
    fi

    IFS='|' read -r container_name service_name <<< "${containers[$container_choice]}"

    # Check if container is running
    if ! sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
        echo -e "\n✗ Container $service_name ($container_name) is not running."

        # Check if it exists but is stopped
        if sudo docker ps -a --format "{{.Names}}" | grep -q "^${container_name}$"; then
            echo "Container exists but is stopped."
            read -p "Do you want to start it first? (y/N): " start_choice
            if [[ $start_choice == "y" || $start_choice == "Y" ]]; then
                start_container "$container_name" "$service_name"
                echo ""
                sleep 3  # Give container time to fully start
            else
                echo "Cannot enter stopped container."
                return
            fi
        else
            echo "Container does not exist."
            return
        fi
    fi

    # Determine the best shell to use
    echo -e "\n=== ENTERING $service_name CONTAINER ==="
    echo "Container: $container_name"
    echo ""

    # Try different shells in order of preference
    local shells=("/bin/bash" "/bin/sh" "/bin/ash")
    local shell_found=false

    for shell in "${shells[@]}"; do
        if sudo docker exec "$container_name" test -x "$shell" 2>/dev/null; then
            echo "Using shell: $shell"
            echo "Type 'exit' to return to the debug menu."
            echo "----------------------------------------"
            sudo docker exec -it "$container_name" "$shell"
            shell_found=true
            break
        fi
    done

    if [ "$shell_found" = false ]; then
        echo "No suitable shell found in container. Trying default command..."
        sudo docker exec -it "$container_name" /bin/sh 2>/dev/null || {
            echo "✗ Failed to enter container. Container may not support interactive shells."
        }
    fi

    echo ""
    echo "=== EXITED FROM $service_name CONTAINER ==="
}

check_logs() {
    local container_name=$1
    local service_name=$2
    local mode=${3:-"static"}
    local lines=${4:-100}

    if [ "$mode" == "static" ]; then
        echo -e "\n=== $service_name LOGS (last $lines lines) ==="
        if sudo docker logs "$container_name" --tail "$lines" 2>/dev/null; then
            echo "✓ Logs retrieved successfully"
        else
            echo "✗ Failed to retrieve logs for $container_name"
            echo "Container may not exist or may be stopped"
        fi
    elif [ "$mode" == "monitor" ]; then
        echo -e "\n=== MONITORING $service_name LOGS (Real-time) ==="
        echo "Starting with last $lines lines, then showing new logs in real-time..."
        echo "Press 'Ctrl+C' or 'q' + Enter to quit monitoring"
        echo "=========================================="

        # Check if container exists and is running
        if ! sudo docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
            echo "✗ Container $container_name is not running. Cannot monitor logs."
            return 1
        fi

        # Start monitoring logs in background
        sudo docker logs -f --tail "$lines" "$container_name" 2>&1 &
        local docker_logs_pid=$!

        # Function to cleanup when user wants to quit
        cleanup_monitor() {
            echo -e "\n\n=== STOPPING LOG MONITORING ==="
            sudo kill $docker_logs_pid 2>/dev/null
            wait $docker_logs_pid 2>/dev/null
            echo "✓ Log monitoring stopped"
        }

        # Set trap to handle Ctrl+C
        trap cleanup_monitor INT

        # Monitor for 'q' input
        while true; do
            read -t 1 -n 1 input 2>/dev/null
            if [ "$input" == "q" ] || [ "$input" == "Q" ]; then
                cleanup_monitor
                break
            fi

            # Check if docker logs process is still running
            if ! kill -0 $docker_logs_pid 2>/dev/null; then
                echo -e "\n✗ Log monitoring stopped unexpectedly"
                break
            fi
        done

        # Reset trap
        trap - INT
    fi
}

show_log_options() {
    local container_name=$1
    local service_name=$2

    echo ""
    echo "Choose log viewing mode:"
    echo "1) Static logs - Show specific number of lines"
    echo "2) Monitor logs - Real-time log monitoring"
    echo ""
    read -p "Enter mode (1 or 2, default: 1): " log_mode
    log_mode=${log_mode:-1}

    case $log_mode in
        1)
            read -p "How many log lines to show? (default: 100): " lines
            lines=${lines:-100}
            check_logs "$container_name" "$service_name" "static" "$lines"
            ;;
        2)
            read -p "How many initial log lines to show? (default: 100): " lines
            lines=${lines:-100}
            echo ""
            echo "Note: After showing initial logs, new logs will appear in real-time."
            echo "You can press 'q' + Enter or Ctrl+C to stop monitoring."
            echo ""
            read -p "Press Enter to start monitoring..."
            check_logs "$container_name" "$service_name" "monitor" "$lines"
            ;;
        *)
            echo "Invalid mode. Using static logs with default 100 lines."
            check_logs "$container_name" "$service_name" "static" "100"
            ;;
    esac
}

check_and_start_containers() {
    echo -e "\n=== CHECKING ALL CONTAINER STATUS ==="
    local stopped_containers=()

    for key in {1..16}; do   # UPDATED
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
    echo "Note: Infrastructure containers will be started first, then application services."
    echo "Each container will wait 30 seconds after starting to ensure proper initialization."
    echo ""
    read -p "Continue? (y/N): " choice
    if [[ ! ($choice == "y" || $choice == "Y") ]]; then
        echo "Cancelled."
        return
    fi

    # Start infrastructure first
    echo -e "\n=== PHASE 1: STARTING INFRASTRUCTURE ==="
    local infra_order=(
        "10|kiwi-mysql|MYSQL"
        "11|kiwi-redis|REDIS"
        "12|kiwi-rabbit|RABBITMQ"
        "14|kiwi-es|ELASTICSEARCH"
        "15|storage|FASTDFS-STORAGE"
        "16|tracker|FASTDFS-TRACKER"
    )

    local total_infra=${#infra_order[@]}
    local current_infra=0

    for item in "${infra_order[@]}"; do
        IFS='|' read -r key container_name service_name <<< "$item"
        ((current_infra++))
        
        echo "[$current_infra/$total_infra] Processing $service_name..."

        if check_container_status "$container_name" "$service_name" >/dev/null 2>&1; then
            echo "✓ $service_name is already running"
        else
            start_container "$container_name" "$service_name"
            if [ $current_infra -lt $total_infra ]; then
                echo "  Waiting 30 seconds for $service_name to fully initialize before starting next container..."
                # Show countdown for better user experience
                for i in {30..1}; do
                    printf "\r  Countdown: %02d seconds remaining..." $i
                    sleep 1
                done
                printf "\r  ✓ Wait complete. Ready for next container.                    \n"
            else
                echo "  Final infrastructure container started. Waiting 10 seconds for stabilization..."
                sleep 10
            fi
        fi
        echo ""
    done

    # Handle Kiwi UI with port check
    echo "=== STARTING KIWI UI ==="
    if check_container_status "kiwi-ui" "KIWI-UI" >/dev/null 2>&1; then
        echo "✓ KIWI-UI is already running"
    else
        if check_port_80_and_handle; then
            start_container "kiwi-ui" "KIWI-UI"
            sleep 3
        else
            echo "⚠ Skipping KIWI-UI startup due to port 80 conflict"
        fi
    fi
    echo ""

    # Start application services
    echo -e "\n=== PHASE 2: STARTING APPLICATION SERVICES ==="
    local app_order=(
        "1|kiwi-base-kiwi-eureka-1|EUREKA"
        "2|kiwi-base-kiwi-config-1|CONFIG"
        "3|kiwi-service-kiwi-upms-1|UPMS"
        "4|kiwi-service-kiwi-auth-1|AUTH"
        "5|kiwi-service-kiwi-gate-1|GATE"
        "6|kiwi-service-kiwi-word-biz-1|WORD-BIZ"
        "7|kiwi-service-kiwi-ai-biz-1|AI-BIZ"
        "8|kiwi-service-kiwi-crawler-1|CRAWLER"
        "9|kiwi-service-kiwi-tools-biz-1|TOOLS-BIZ"   # NEW
    )

    local total_apps=${#app_order[@]}
    local current_app=0

    for item in "${app_order[@]}"; do
        IFS='|' read -r key container_name service_name <<< "$item"
        ((current_app++))
        
        echo "[$current_app/$total_apps] Processing $service_name..."

        if check_container_status "$container_name" "$service_name" >/dev/null 2>&1; then
            echo "✓ $service_name is already running"
        else
            start_container "$container_name" "$service_name"
            if [ $current_app -lt $total_apps ]; then
                echo "  Waiting 30 seconds for $service_name to fully initialize before starting next container..."
                # Show countdown for better user experience
                for i in {30..1}; do
                    printf "\r  Countdown: %02d seconds remaining..." $i
                    sleep 1
                done
                printf "\r  ✓ Wait complete. Ready for next container.                    \n"
            else
                echo "  Final application container started. Waiting 5 seconds for stabilization..."
                sleep 5
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

    for key in {1..16}; do   # UPDATED
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
    echo "Choose log viewing mode for all containers:"
    echo "1) Static logs - Show specific number of lines for each"
    echo "2) Monitor logs - Monitor one container at a time"
    echo ""
    read -p "Enter mode (1 or 2, default: 1): " log_mode
    log_mode=${log_mode:-1}

    if [ "$log_mode" == "1" ]; then
        read -p "How many log lines to show for each container? (default: 30): " lines
        lines=${lines:-30}

        for key in {1..9}; do    # UPDATED (was 1..8)
            IFS='|' read -r container_name service_name <<< "${containers[$key]}"
            check_logs "$container_name" "$service_name" "static" "$lines"
            echo ""
        done
    elif [ "$log_mode" == "2" ]; then
        read -p "How many initial log lines to show? (default: 50): " lines
        lines=${lines:-50}

        echo ""
        echo "Choose which service container to monitor:"
        for key in {1..9}; do    # UPDATED (was 1..8)
            IFS='|' read -r container_name service_name <<< "${containers[$key]}"
            echo "$key) $service_name ($container_name)"
        done
        echo ""
        read -p "Enter container number (1-9): " container_choice  # UPDATED

        if [[ $container_choice =~ ^[1-9]$ ]]; then  # UPDATED (was ^[1-8$])
            IFS='|' read -r container_name service_name <<< "${containers[$container_choice]}"
            echo ""
            echo "Note: Press 'q' + Enter or Ctrl+C to stop monitoring."
            echo ""
            read -p "Press Enter to start monitoring $service_name..."
            check_logs "$container_name" "$service_name" "monitor" "$lines"
        else
            echo "Invalid choice. Showing static logs for all containers instead."
            for key in {1..9}; do
                IFS='|' read -r container_name service_name <<< "${containers[$key]}"
                check_logs "$container_name" "$service_name" "static" "30"
                echo ""
            done
        fi
    else
        echo "Invalid mode. Using static logs with 30 lines for each container."
        for key in {1..9}; do
            IFS='|' read -r container_name service_name <<< "${containers[$key]}"
            check_logs "$container_name" "$service_name" "static" "30"
            echo ""
        done
    fi
}

# NEW: Multi-pane tmux dashboard for live logs
logs_dashboard() {
    echo -e "\n=== MULTI-PANE LIVE LOGS DASHBOARD (SERVICES 1-9) ==="
    echo "This opens a tmux session with panes for each service showing: docker logs -f (all logs)"
    echo "- Detach: Ctrl+b then d | Close pane: Ctrl+b then x | Resize: Ctrl+b then arrow keys"

    # Check tmux availability
    if ! command -v tmux >/dev/null 2>&1; then
        echo "✗ tmux is not installed or not found in PATH."
        echo "Install on macOS (Homebrew): brew install tmux"
        echo "Falling back to quick health logs (static last 50 lines)."
        quick_health_logs
        return
    fi

    local session="kiwi-logs"

    # If session already exists, kill to rebuild
    if tmux has-session -t "$session" 2>/dev/null; then
        tmux kill-session -t "$session" 2>/dev/null
    fi

    # Helper to build the per-pane command
    build_pane_cmd() {
        local c_name="$1"
        local s_name="$2"
        # Use bash -lc and embed container/service names at generation time
        echo "bash -lc 'wait_secs=0; \
while true; do \
  if sudo docker ps --format \"{{.Names}}\" | grep -q \"^${c_name}$\"; then \
    echo \"=== ${s_name} (${c_name}) logs (ALL + follow) ===\"; \
    sudo docker logs -f ${c_name}; \
    echo \"[${s_name}] docker logs exited. Restarting in 3s...\"; \
    sleep 3; \
  else \
    wait_secs=$((wait_secs+1)); \
    clear; \
    echo \"[${s_name}] waiting for container ${c_name} to start... (${wait_secs}s)\"; \
    eureka_status=$(sudo docker ps --format \"{{.Names}}\" | grep -q \"^kiwi-base-kiwi-eureka-1$\" && echo up || echo down); \
    config_status=$(sudo docker ps --format \"{{.Names}}\" | grep -q \"^kiwi-base-kiwi-config-1$\" && echo up || echo down); \
    gate_status=$(sudo docker ps --format \"{{.Names}}\" | grep -q \"^kiwi-service-kiwi-gate-1$\" && echo up || echo down); \
    echo \"Core health: eureka:${eureka_status} config:${config_status} gate:${gate_status}\"; \
    if [ \"${c_name}\" = \"kiwi-service-kiwi-upms-1\" ] && [ \"$gate_status\" = \"up\" ]; then \
       echo \"kiwi-gate is healthy. Starting kiwi-upms...\"; \
    fi; \
    sleep 1; \
  fi; \
done'"
    }

    local first=true
    for key in {1..9}; do
        IFS='|' read -r container_name service_name <<< "${containers[$key]}"
        pane_cmd=$(build_pane_cmd "$container_name" "$service_name")
        if $first; then
            tmux new-session -d -s "$session" "$pane_cmd"
            first=false
        else
            tmux split-window -t "$session" -v "$pane_cmd"
        fi
        tmux select-layout -t "$session" tiled >/dev/null 2>&1
    done

    # Improve usability
    tmux set-option -t "$session" status on >/dev/null 2>&1
    tmux set-option -t "$session" mouse on >/dev/null 2>&1
    tmux select-layout -t "$session" tiled >/dev/null 2>&1

    echo "Launching tmux dashboard..."
    sleep 1
    tmux attach-session -t "$session"
}

quick_health_logs() {
    echo -e "\n=== QUICK HEALTH CHECK: LAST 50 LINES FROM ALL SERVICE CONTAINERS ==="
    echo "This will show the last 50 log lines for each Kiwi microservice (1-9)."
    echo ""
    for key in {1..9}; do
        IFS='|' read -r container_name service_name <<< "${containers[$key]}"
        check_logs "$container_name" "$service_name" "static" "50"
        echo ""
    done
    echo "=== END OF QUICK HEALTH CHECK ==="
}

# Main loop
while true; do
    show_menu
    read -p "Enter your choice (0-27): " choice   # UPDATED

    case $choice in
        0)
            echo "Exiting..."
            exit 0
            ;;
        1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16)
            IFS='|' read -r container_name service_name <<< "${containers[$choice]}"

            # First check if container is running
            echo -e "\n=== CHECKING $service_name STATUS ==="
            status_code=0
            check_container_status "$container_name" "$service_name" || status_code=$?

            if [ $status_code -eq 1 ]; then  # Container exists but is stopped
                echo ""
                read -p "Container is stopped. Do you want to start it? (y/N): " start_choice
                if [[ $start_choice == "y" || $start_choice == "Y" ]]; then
                    # Special handling for kiwi-ui
                    if [ "$container_name" == "kiwi-ui" ]; then
                        if check_port_80_and_handle; then
                            start_container "$container_name" "$service_name"
                        else
                            echo "⚠ Cannot start kiwi-ui due to port 80 conflict"
                        fi
                    else
                        start_container "$container_name" "$service_name"
                    fi
                fi
            elif [ $status_code -eq 2 ]; then  # Container doesn't exist
                echo "Cannot show logs - container does not exist."
                echo ""
                read -p "Press Enter to continue..."
                continue
            fi

            # Show logs if container is running or user chose not to start
            show_log_options "$container_name" "$service_name"
            ;;
        17) check_all_failed ;;
        18) check_connectivity ;;
        19) show_resource_usage ;;
        20) show_container_status ;;
        21) check_and_start_containers ;;
        22) start_all_kiwi_containers ;;
        23) start_infrastructure_containers ;;
        24) enter_container ;;
        25) stop_single_container ;;
        26) restart_single_container ;;
        27) logs_dashboard ;;  # UPDATED to tmux dashboard
        *) echo "Invalid choice. Please enter a number between 0-27." ;;
    esac

    echo ""
    read -p "Press Enter to continue..."

done
