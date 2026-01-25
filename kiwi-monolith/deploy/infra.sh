#!/bin/bash
# Infrastructure management script for Kiwi Monolith
# Usage: ./infra.sh [start|stop|restart|status|logs|clean|backup-db|restore-db|backup-ftp|restore-ftp]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Load environment file if exists
if [ -f ".env" ]; then
    export $(grep -v '^#' .env | xargs)
fi

# Backup directory
BACKUP_DIR="${BACKUP_DIR:-$SCRIPT_DIR/backups}"
mkdir -p "$BACKUP_DIR"

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

# ==================== Database Backup/Restore ====================

backup_db() {
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="$BACKUP_DIR/kiwi_db_${TIMESTAMP}.sql.gz"

    log_info "Starting MySQL database backup..."

    # Check if MySQL container is running
    if ! docker ps --format '{{.Names}}' | grep -q '^kiwi-mysql$'; then
        log_error "MySQL container is not running. Please start infrastructure first."
        exit 1
    fi

    # Create backup using mysqldump
    docker exec kiwi-mysql mysqldump \
        -u root \
        -p"${DB_PASSWORD:-kiwi123}" \
        --single-transaction \
        --routines \
        --triggers \
        --databases "${DB_NAME:-kiwi_db}" \
        2>/dev/null | gzip > "$BACKUP_FILE"

    if [ -s "$BACKUP_FILE" ]; then
        BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
        log_info "Database backup completed successfully!"
        log_info "Backup file: $BACKUP_FILE ($BACKUP_SIZE)"
    else
        rm -f "$BACKUP_FILE"
        log_error "Backup failed - empty backup file"
        exit 1
    fi

    # List recent backups
    echo ""
    log_info "Recent database backups:"
    ls -lh "$BACKUP_DIR"/kiwi_db_*.sql.gz 2>/dev/null | tail -5 || echo "  No backups found"
}

restore_db() {
    BACKUP_FILE="${2:-}"

    if [ -z "$BACKUP_FILE" ]; then
        # List available backups
        echo ""
        log_info "Available database backups:"
        echo "----------------------------------------"
        ls -lht "$BACKUP_DIR"/kiwi_db_*.sql.gz 2>/dev/null || { log_error "No backups found in $BACKUP_DIR"; exit 1; }
        echo "----------------------------------------"
        echo ""
        log_warn "Usage: $0 restore-db <backup_file>"
        log_info "Example: $0 restore-db $BACKUP_DIR/kiwi_db_20240101_120000.sql.gz"
        exit 1
    fi

    # Check if backup file exists
    if [ ! -f "$BACKUP_FILE" ]; then
        log_error "Backup file not found: $BACKUP_FILE"
        exit 1
    fi

    # Check if MySQL container is running
    if ! docker ps --format '{{.Names}}' | grep -q '^kiwi-mysql$'; then
        log_error "MySQL container is not running. Please start infrastructure first."
        exit 1
    fi

    log_warn "This will OVERWRITE the current database! Are you sure? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        log_info "Restore cancelled."
        exit 0
    fi

    log_info "Restoring database from: $BACKUP_FILE"

    # Restore from backup
    gunzip -c "$BACKUP_FILE" | docker exec -i kiwi-mysql mysql \
        -u root \
        -p"${DB_PASSWORD:-kiwi123}" \
        2>/dev/null

    if [ $? -eq 0 ]; then
        log_info "Database restore completed successfully!"
    else
        log_error "Database restore failed!"
        exit 1
    fi
}

# ==================== FTP Backup/Restore ====================

backup_ftp() {
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="$BACKUP_DIR/kiwi_ftp_${TIMESTAMP}.tar.gz"
    FTP_HOST="${FTP_HOST:-localhost}"
    FTP_PORT="${FTP_PORT:-21}"
    FTP_USER="${FTP_USER:-ftpuser}"
    FTP_PASS="${FTP_PASS:-ftppass}"
    FTP_REMOTE_DIR="${FTP_REMOTE_DIR:-/}"
    FTP_LOCAL_TEMP="$BACKUP_DIR/.ftp_temp_$$"

    log_info "Starting FTP backup..."
    log_info "FTP Host: $FTP_HOST:$FTP_PORT"
    log_info "Remote directory: $FTP_REMOTE_DIR"

    # Create temp directory
    mkdir -p "$FTP_LOCAL_TEMP"

    # Download all files from FTP using lftp (preferred) or curl
    log_info "Downloading files from FTP server..."

    if command -v lftp &> /dev/null; then
        lftp -u "$FTP_USER","$FTP_PASS" "ftp://$FTP_HOST:$FTP_PORT" -e "mirror $FTP_REMOTE_DIR $FTP_LOCAL_TEMP; quit" 2>/dev/null
        RESULT=$?
    else
        # Fallback to curl
        log_warn "lftp not found, using curl"
        cd "$FTP_LOCAL_TEMP"
        curl -s -u "$FTP_USER:$FTP_PASS" "ftp://$FTP_HOST:$FTP_PORT$FTP_REMOTE_DIR" -o listing.txt 2>/dev/null
        # Parse listing and download files (basic implementation)
        while IFS= read -r line; do
            filename=$(echo "$line" | awk '{print $NF}')
            if [ -n "$filename" ] && [ "$filename" != "." ] && [ "$filename" != ".." ]; then
                curl -s -u "$FTP_USER:$FTP_PASS" "ftp://$FTP_HOST:$FTP_PORT$FTP_REMOTE_DIR/$filename" -o "$filename" 2>/dev/null
            fi
        done < listing.txt
        rm -f listing.txt
        cd "$SCRIPT_DIR"
        RESULT=0
    fi

    # Create tar archive
    if [ -d "$FTP_LOCAL_TEMP" ] && [ "$(ls -A "$FTP_LOCAL_TEMP" 2>/dev/null)" ]; then
        tar -czf "$BACKUP_FILE" -C "$FTP_LOCAL_TEMP" .
        BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
        log_info "FTP backup completed successfully!"
        log_info "Backup file: $BACKUP_FILE ($BACKUP_SIZE)"
    else
        log_error "No files downloaded from FTP server"
        rm -rf "$FTP_LOCAL_TEMP"
        exit 1
    fi

    # Cleanup temp directory
    rm -rf "$FTP_LOCAL_TEMP"

    # List recent backups
    echo ""
    log_info "Recent FTP backups:"
    ls -lh "$BACKUP_DIR"/kiwi_ftp_*.tar.gz 2>/dev/null | tail -5 || echo "  No backups found"
}

restore_ftp() {
    BACKUP_FILE="${2:-}"
    FTP_HOST="${FTP_HOST:-localhost}"
    FTP_PORT="${FTP_PORT:-21}"
    FTP_USER="${FTP_USER:-ftpuser}"
    FTP_PASS="${FTP_PASS:-ftppass}"
    FTP_REMOTE_DIR="${FTP_REMOTE_DIR:-/}"
    FTP_LOCAL_TEMP="$BACKUP_DIR/.ftp_temp_$$"

    if [ -z "$BACKUP_FILE" ]; then
        # List available backups
        echo ""
        log_info "Available FTP backups:"
        echo "----------------------------------------"
        ls -lht "$BACKUP_DIR"/kiwi_ftp_*.tar.gz 2>/dev/null || { log_error "No backups found in $BACKUP_DIR"; exit 1; }
        echo "----------------------------------------"
        echo ""
        log_warn "Usage: $0 restore-ftp <backup_file>"
        log_info "Example: $0 restore-ftp $BACKUP_DIR/kiwi_ftp_20240101_120000.tar.gz"
        exit 1
    fi

    # Check if backup file exists
    if [ ! -f "$BACKUP_FILE" ]; then
        log_error "Backup file not found: $BACKUP_FILE"
        exit 1
    fi

    log_warn "This will upload files to FTP server at $FTP_HOST:$FTP_PORT$FTP_REMOTE_DIR"
    log_warn "Existing files may be OVERWRITTEN! Are you sure? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        log_info "Restore cancelled."
        exit 0
    fi

    log_info "Restoring FTP from: $BACKUP_FILE"
    log_info "Target FTP: $FTP_HOST:$FTP_PORT$FTP_REMOTE_DIR"

    # Extract to temp directory
    mkdir -p "$FTP_LOCAL_TEMP"
    tar -xzf "$BACKUP_FILE" -C "$FTP_LOCAL_TEMP"

    # Upload files to FTP
    if command -v lftp &> /dev/null; then
        lftp -u "$FTP_USER","$FTP_PASS" "ftp://$FTP_HOST:$FTP_PORT" -e "mirror -R $FTP_LOCAL_TEMP $FTP_REMOTE_DIR; quit" 2>/dev/null
        RESULT=$?
    else
        # Fallback: upload files one by one with curl
        log_warn "lftp not found, using curl for upload"
        RESULT=0
        find "$FTP_LOCAL_TEMP" -type f | while read file; do
            RELATIVE_PATH="${file#$FTP_LOCAL_TEMP/}"
            curl -s -T "$file" -u "$FTP_USER:$FTP_PASS" "ftp://$FTP_HOST:$FTP_PORT$FTP_REMOTE_DIR/$RELATIVE_PATH" --ftp-create-dirs 2>/dev/null || RESULT=1
        done
    fi

    # Cleanup
    rm -rf "$FTP_LOCAL_TEMP"

    if [ $RESULT -eq 0 ]; then
        log_info "FTP restore completed successfully!"
    else
        log_error "FTP restore failed!"
        exit 1
    fi
}

list_backups() {
    echo ""
    log_info "=== Database Backups ==="
    ls -lht "$BACKUP_DIR"/kiwi_db_*.sql.gz 2>/dev/null || echo "  No database backups found"
    echo ""
    log_info "=== FTP Backups ==="
    ls -lht "$BACKUP_DIR"/kiwi_ftp_*.tar.gz 2>/dev/null || echo "  No FTP backups found"
    echo ""
    log_info "Backup directory: $BACKUP_DIR"
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
    backup-db)
        backup_db
        ;;
    restore-db)
        restore_db "$@"
        ;;
    backup-ftp)
        backup_ftp
        ;;
    restore-ftp)
        restore_ftp "$@"
        ;;
    backups)
        list_backups
        ;;
    *)
        echo "Kiwi Monolith Infrastructure Manager"
        echo ""
        echo "Usage: $0 {start|stop|restart|status|logs|clean|backup-db|restore-db|backup-ftp|restore-ftp|backups}"
        echo ""
        echo "Commands:"
        echo "  start      - Start all infrastructure services"
        echo "  stop       - Stop all infrastructure services"
        echo "  restart    - Restart all infrastructure services"
        echo "  status     - Show status of all services"
        echo "  logs       - Show logs (optionally: logs <service>)"
        echo "  clean      - Stop and remove all data (WARNING: destroys data)"
        echo ""
        echo "Backup/Restore:"
        echo "  backup-db  - Create a MySQL database backup"
        echo "  restore-db - Restore MySQL from backup (restore-db <file>)"
        echo "  backup-ftp - Download and backup files from FTP server"
        echo "  restore-ftp- Upload backup files to FTP server (restore-ftp <file>)"
        echo "  backups    - List all available backups"
        echo ""
        echo "Environment Variables:"
        echo "  BACKUP_DIR     - Backup directory (default: ./backups)"
        echo "  FTP_HOST       - FTP server hostname (default: localhost)"
        echo "  FTP_PORT       - FTP server port (default: 21)"
        echo "  FTP_USER       - FTP username (default: ftpuser)"
        echo "  FTP_PASS       - FTP password (default: ftppass)"
        echo "  FTP_REMOTE_DIR - Remote directory to backup (default: /)"
        echo ""
        exit 1
        ;;
esac
