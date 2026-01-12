#!/bin/bash
# Local Infrastructure Startup Script
# Starts MySQL, Redis, Elasticsearch, and FTP containers for local development
# Usage: ./local-infra-start.sh

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ $1${NC}"; }

# Configuration - customize these as needed
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-kiwi123}"
REDIS_PASSWORD="${REDIS_PASSWORD:-kiwi123}"
ES_ROOT_PASSWORD="${ES_ROOT_PASSWORD:-kiwi123}"
ES_USER_NAME="${ES_USER_NAME:-kiwi}"
ES_USER_PASSWORD="${ES_USER_PASSWORD:-kiwi123}"
FTP_USER="${FTP_USER:-ftpuser}"
FTP_PASS="${FTP_PASS:-ftp123}"

# Data directories (relative to user's home)
DATA_BASE_DIR="${DATA_BASE_DIR:-$HOME/docker-local}"
MYSQL_DATA_DIR="$DATA_BASE_DIR/mysql"
FTP_DATA_DIR="$DATA_BASE_DIR/ftp-data"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==========================================="
echo "  Local Infrastructure Startup Script"
echo "==========================================="
echo "Data directory: $DATA_BASE_DIR"
echo ""

# Check Docker is available
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed or not in PATH"
    exit 1
fi

if ! docker info &> /dev/null; then
    print_error "Docker daemon is not running"
    exit 1
fi

print_success "Docker is available"

# Create data directories
print_info "Creating data directories..."
mkdir -p "$MYSQL_DATA_DIR" "$FTP_DATA_DIR"
print_success "Data directories created"

# ============================================
# 1. MySQL 8.0
# ============================================
start_mysql() {
    print_info "Starting MySQL 8.0..."

    if docker ps --format '{{.Names}}' | grep -q '^kiwi-mysql$'; then
        print_warning "kiwi-mysql is already running"
        return 0
    fi

    # Remove stopped container if exists
    docker rm -f kiwi-mysql 2>/dev/null || true

    docker run -d \
        --name kiwi-mysql \
        -p 3307:3306 \
        -v "$MYSQL_DATA_DIR:/var/lib/mysql" \
        -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
        -e MYSQL_DATABASE=kiwi_db \
        --restart=unless-stopped \
        mysql:8.0 \
        --character-set-server=utf8mb4 \
        --collation-server=utf8mb4_unicode_ci \
        --max-connections=1000

    print_info "Waiting for MySQL to be ready..."
    for i in {1..30}; do
        if docker exec kiwi-mysql mysqladmin ping -h localhost -u root -p"$MYSQL_ROOT_PASSWORD" &>/dev/null; then
            print_success "MySQL is ready"
            return 0
        fi
        sleep 2
    done
    print_warning "MySQL may not be fully ready yet"
}

# ============================================
# 2. Redis
# ============================================
start_redis() {
    print_info "Starting Redis..."

    if docker ps --format '{{.Names}}' | grep -q '^kiwi-redis$'; then
        print_warning "kiwi-redis is already running"
        return 0
    fi

    docker rm -f kiwi-redis 2>/dev/null || true

    docker run -d \
        --name kiwi-redis \
        -p 6380:6379 \
        --restart=unless-stopped \
        redis:latest \
        --requirepass "$REDIS_PASSWORD"

    sleep 2
    if docker ps --format '{{.Names}}' | grep -q '^kiwi-redis$'; then
        print_success "Redis is running"
    else
        print_error "Failed to start Redis"
    fi
}

# ============================================
# 3. Elasticsearch 7.17.9
# ============================================
start_elasticsearch() {
    print_info "Starting Elasticsearch 7.17.9..."

    if docker ps --format '{{.Names}}' | grep -q '^kiwi-es$'; then
        print_warning "kiwi-es is already running"
        return 0
    fi

    docker rm -f kiwi-es 2>/dev/null || true
    docker volume rm es_config es_data 2>/dev/null || true

    docker run -d \
        --name kiwi-es \
        -p 9201:9200 \
        -p 9301:9300 \
        --hostname kiwi-es \
        -e "discovery.type=single-node" \
        -e "xpack.security.enabled=true" \
        -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
        -v es_config:/usr/share/elasticsearch/config \
        -v es_data:/usr/share/elasticsearch/data \
        --restart=unless-stopped \
        elasticsearch:7.17.9

    print_info "Waiting for Elasticsearch to start (this may take a minute)..."
    sleep 15

    # Create users
    print_info "Creating Elasticsearch users..."
    docker exec -u elasticsearch kiwi-es /usr/share/elasticsearch/bin/elasticsearch-users useradd root -p "$ES_ROOT_PASSWORD" -r superuser 2>/dev/null \
        || docker exec -u elasticsearch kiwi-es /usr/share/elasticsearch/bin/elasticsearch-users passwd root -p "$ES_ROOT_PASSWORD" 2>/dev/null || true

    docker exec -u elasticsearch kiwi-es /usr/share/elasticsearch/bin/elasticsearch-users useradd "$ES_USER_NAME" -p "$ES_USER_PASSWORD" -r superuser 2>/dev/null \
        || docker exec -u elasticsearch kiwi-es /usr/share/elasticsearch/bin/elasticsearch-users passwd "$ES_USER_NAME" -p "$ES_USER_PASSWORD" 2>/dev/null || true

    # Wait for API
    print_info "Waiting for Elasticsearch API..."
    for i in {1..30}; do
        if curl -s -u "root:$ES_ROOT_PASSWORD" "http://localhost:9201" &>/dev/null; then
            print_success "Elasticsearch is ready"

            # Create index if not exists
            if ! curl -s -u "root:$ES_ROOT_PASSWORD" -I "http://localhost:9201/kiwi_vocabulary" 2>/dev/null | grep -q "200 OK"; then
                print_info "Creating kiwi_vocabulary index..."
                curl -s -u "root:$ES_ROOT_PASSWORD" -H "Content-Type: application/json" -X PUT "http://localhost:9201/kiwi_vocabulary" -d '{
                    "settings": {
                        "number_of_shards": 1,
                        "number_of_replicas": 0
                    },
                    "mappings": {
                        "properties": {
                            "wordId": {"type": "integer"},
                            "wordName": {"type": "keyword"},
                            "isCollect": {"type": "keyword"},
                            "isLogin": {"type": "keyword"},
                            "characterVOList": {"type": "nested"}
                        }
                    }
                }' >/dev/null 2>&1
                print_success "Index created"
            fi
            return 0
        fi
        sleep 2
    done
    print_warning "Elasticsearch may not be fully ready"
}

# ============================================
# 4. FTP Server (vsftpd)
# ============================================
start_ftp() {
    print_info "Starting FTP Server..."

    if docker ps --format '{{.Names}}' | grep -q '^kiwi-ftp$'; then
        print_warning "kiwi-ftp is already running"
        return 0
    fi

    docker rm -f kiwi-ftp 2>/dev/null || true

    # Build FTP image if Dockerfile exists
    FTP_DOCKERFILE="$SCRIPT_DIR/ftp/Dockerfile"
    if [ -f "$FTP_DOCKERFILE" ]; then
        print_info "Building FTP image..."
        docker build -t kiwi-ftp:1.1 "$SCRIPT_DIR/ftp/"
    else
        print_warning "FTP Dockerfile not found at $FTP_DOCKERFILE, using fauria/vsftpd instead"
        docker pull fauria/vsftpd
        docker run -d \
            --name kiwi-ftp \
            -p 22:21 \
            -p 21101-21111:21100-21110 \
            -v "$FTP_DATA_DIR:/home/vsftpd/$FTP_USER" \
            -e FTP_USER="$FTP_USER" \
            -e FTP_PASS="$FTP_PASS" \
            -e PASV_MIN_PORT=21100 \
            -e PASV_MAX_PORT=21110 \
            --restart=unless-stopped \
            fauria/vsftpd
        return 0
    fi

    # Detect OS for network mode
    OS=$(uname -s)
    if [ "$OS" = "Darwin" ]; then
        # macOS - use port mapping
        PASV_ADDRESS=$(ipconfig getifaddr en0 2>/dev/null || echo "127.0.0.1")
        docker run -d \
            --name kiwi-ftp \
            -p 22:21 \
            -p 21101-21111:21100-21110 \
            -v "$FTP_DATA_DIR:/home/$FTP_USER" \
            -e FTP_USER="$FTP_USER" \
            -e FTP_PASS="$FTP_PASS" \
            -e PASV_ADDRESS="$PASV_ADDRESS" \
            --restart=unless-stopped \
            kiwi-ftp:1.1
    else
        # Linux - use host network
        docker run -d \
            --name kiwi-ftp \
            --network host \
            -v "$FTP_DATA_DIR:/home/$FTP_USER" \
            -e FTP_USER="$FTP_USER" \
            -e FTP_PASS="$FTP_PASS" \
            --restart=unless-stopped \
            kiwi-ftp:1.1
    fi

    sleep 2
    if docker ps --format '{{.Names}}' | grep -q '^kiwi-ftp$'; then
        print_success "FTP Server is running"
    else
        print_error "Failed to start FTP Server"
    fi
}

# ============================================
# Main execution
# ============================================
echo ""
echo "Starting infrastructure services..."
echo ""

start_mysql
echo ""
start_redis
echo ""
start_elasticsearch
echo ""
start_ftp
echo ""

# ============================================
# Summary
# ============================================
echo ""
echo "==========================================="
echo "  Infrastructure Status"
echo "==========================================="

CONTAINERS=("kiwi-mysql" "kiwi-redis" "kiwi-es" "kiwi-ftp")
RUNNING_COUNT=0

for container in "${CONTAINERS[@]}"; do
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        STATUS=$(docker ps --filter "name=^${container}$" --format "{{.Status}}" | head -1)
        print_success "$container - RUNNING ($STATUS)"
        ((RUNNING_COUNT++))
    else
        print_error "$container - NOT RUNNING"
    fi
done

echo ""
echo "Running: $RUNNING_COUNT/${#CONTAINERS[@]} containers"
echo ""
echo "==========================================="
echo "  Connection Information"
echo "==========================================="
echo ""
echo "MySQL:"
echo "  Host: localhost:3307"
echo "  User: root"
echo "  Password: $MYSQL_ROOT_PASSWORD"
echo "  Database: kiwi_db"
echo ""
echo "Redis:"
echo "  Host: localhost:6380"
echo "  Password: $REDIS_PASSWORD"
echo ""
echo "Elasticsearch:"
echo "  URL: http://localhost:9201"
echo "  User: root / $ES_ROOT_PASSWORD"
echo "  User: $ES_USER_NAME / $ES_USER_PASSWORD"
echo ""
echo "FTP:"
echo "  Host: localhost:22"
echo "  User: $FTP_USER"
echo "  Password: $FTP_PASS"
echo "  Data Dir: $FTP_DATA_DIR"
echo ""
echo "==========================================="
