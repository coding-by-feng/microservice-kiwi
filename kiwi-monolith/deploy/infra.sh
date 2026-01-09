#!/bin/bash
# Infrastructure management script for Kiwi Monolith
# Usage: ./infra.sh [start|stop|restart|status|logs|clean]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Load environment file if exists
if [ -f ".env" ]; then
    export $(grep -v '^#' .env | xargs)
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

start_infra() {
    log_info "Starting infrastructure services..."
    docker-compose up -d

    log_info "Waiting for services to be healthy..."
    sleep 5

    # Wait for MySQL
    log_info "Waiting for MySQL..."
    until docker exec kiwi-mysql mysqladmin ping -h localhost -u root -p${DB_PASSWORD:-kiwi123} --silent 2>/dev/null; do
        sleep 2
    done
    log_info "MySQL is ready!"

    # Wait for Redis
    log_info "Waiting for Redis..."
    until docker exec kiwi-redis redis-cli -a ${REDIS_PASSWORD:-kiwi123} ping 2>/dev/null | grep -q PONG; do
        sleep 2
    done
    log_info "Redis is ready!"

    # Wait for Elasticsearch
    log_info "Waiting for Elasticsearch..."
    until curl -s -u elastic:${ES_PASSWORD:-changeme} http://localhost:${ES_PORT:-9201}/_cluster/health 2>/dev/null | grep -q '"status"'; do
        sleep 3
    done
    log_info "Elasticsearch is ready!"

    # Create Elasticsearch index if not exists
    create_es_index

    log_info "All infrastructure services are running!"
    show_status
}

stop_infra() {
    log_info "Stopping infrastructure services..."
    docker-compose down
    log_info "Infrastructure stopped."
}

restart_infra() {
    stop_infra
    start_infra
}

show_status() {
    echo ""
    log_info "Infrastructure Status:"
    echo "----------------------------------------"
    docker-compose ps
    echo ""
    echo "Connection Info:"
    echo "  MySQL:         localhost:${DB_PORT:-3307} (user: root, password: ${DB_PASSWORD:-kiwi123})"
    echo "  Redis:         localhost:${REDIS_PORT:-6380} (password: ${REDIS_PASSWORD:-kiwi123})"
    echo "  Elasticsearch: http://localhost:${ES_PORT:-9201} (user: elastic, password: ${ES_PASSWORD:-changeme})"
    echo ""
}

show_logs() {
    SERVICE=${2:-}
    if [ -z "$SERVICE" ]; then
        docker-compose logs -f
    else
        docker-compose logs -f "$SERVICE"
    fi
}

clean_infra() {
    log_warn "This will remove all data volumes! Are you sure? (y/N)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        log_info "Stopping and removing containers and volumes..."
        docker-compose down -v
        log_info "Cleanup complete."
    else
        log_info "Cleanup cancelled."
    fi
}

create_es_index() {
    ES_URL="http://localhost:${ES_PORT:-9201}"
    ES_AUTH="elastic:${ES_PASSWORD:-changeme}"
    INDEX_NAME="kiwi_vocabulary"

    # Check if index exists
    if curl -s -u "$ES_AUTH" "$ES_URL/$INDEX_NAME" 2>/dev/null | grep -q "\"$INDEX_NAME\""; then
        log_info "Elasticsearch index '$INDEX_NAME' already exists."
    else
        log_info "Creating Elasticsearch index '$INDEX_NAME'..."
        curl -s -X PUT -u "$ES_AUTH" "$ES_URL/$INDEX_NAME" \
            -H "Content-Type: application/json" \
            -d '{
                "settings": {
                    "number_of_shards": 1,
                    "number_of_replicas": 0,
                    "analysis": {
                        "analyzer": {
                            "kiwi_analyzer": {
                                "type": "custom",
                                "tokenizer": "standard",
                                "filter": ["lowercase", "asciifolding"]
                            }
                        }
                    }
                },
                "mappings": {
                    "properties": {
                        "wordName": { "type": "text", "analyzer": "kiwi_analyzer" },
                        "wordId": { "type": "long" },
                        "infoType": { "type": "integer" },
                        "paraphraseText": { "type": "text", "analyzer": "kiwi_analyzer" }
                    }
                }
            }' > /dev/null
        log_info "Elasticsearch index created."
    fi
}

# Main
case "${1:-}" in
    start)
        start_infra
        ;;
    stop)
        stop_infra
        ;;
    restart)
        restart_infra
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs "$@"
        ;;
    clean)
        clean_infra
        ;;
    *)
        echo "Kiwi Monolith Infrastructure Manager"
        echo ""
        echo "Usage: $0 {start|stop|restart|status|logs|clean}"
        echo ""
        echo "Commands:"
        echo "  start   - Start all infrastructure services"
        echo "  stop    - Stop all infrastructure services"
        echo "  restart - Restart all infrastructure services"
        echo "  status  - Show status of all services"
        echo "  logs    - Show logs (optionally: logs <service>)"
        echo "  clean   - Stop and remove all data (WARNING: destroys data)"
        echo ""
        exit 1
        ;;
esac
