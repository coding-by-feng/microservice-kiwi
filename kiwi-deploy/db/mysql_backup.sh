#!/bin/bash

# MySQL Container Backup Script
# This script creates backups of all databases in the kiwi-mysql container

# Configuration
CONTAINER_NAME="kiwi-mysql"
BACKUP_DIR="/home/kason/mysql_backups"
DATE=$(date +"%Y%m%d_%H%M%S")
MYSQL_USER="root"  # Change this to your MySQL user
MYSQL_PASSWORD=""  # You'll be prompted or set this

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if container is running
check_container() {
    if ! sudo docker ps --format "table {{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
        print_error "Container '${CONTAINER_NAME}' is not running!"
        exit 1
    fi
    print_status "Container '${CONTAINER_NAME}' is running"
}

# Function to create backup directory
create_backup_dir() {
    if [ ! -d "$BACKUP_DIR" ]; then
        mkdir -p "$BACKUP_DIR"
        print_status "Created backup directory: $BACKUP_DIR"
    fi
}

# Function to get MySQL password
get_mysql_password() {
    if [ -z "$MYSQL_PASSWORD" ]; then
        echo -n "Enter MySQL password for user '$MYSQL_USER': "
        read -s MYSQL_PASSWORD
        echo
    fi
}

# Function to test MySQL connection
test_connection() {
    print_status "Testing MySQL connection..."
    if sudo docker exec "$CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" &>/dev/null; then
        print_status "MySQL connection successful"
    else
        print_error "Failed to connect to MySQL. Please check your credentials."
        exit 1
    fi
}

# Function to get list of databases (excluding system databases)
get_databases() {
    print_status "Getting list of databases..."
    DATABASES=$(sudo docker exec "$CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SHOW DATABASES;" | grep -Ev "^(Database|information_schema|performance_schema|mysql|sys)$")

    if [ -z "$DATABASES" ]; then
        print_warning "No user databases found to backup"
        exit 0
    fi

    print_status "Found databases: $(echo $DATABASES | tr '\n' ' ')"
}

# Function to backup individual database
backup_database() {
    local db_name=$1
    local backup_file="${BACKUP_DIR}/${db_name}_${DATE}.sql"

    print_status "Backing up database: $db_name"

    if sudo docker exec "$CONTAINER_NAME" mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --single-transaction --routines --triggers "$db_name" > "$backup_file"; then
        # Compress the backup
        gzip "$backup_file"
        print_status "Database '$db_name' backed up to: ${backup_file}.gz"

        # Get file size
        local file_size=$(du -h "${backup_file}.gz" | cut -f1)
        print_status "Backup size: $file_size"
    else
        print_error "Failed to backup database: $db_name"
        return 1
    fi
}

# Function to backup all databases in one file
backup_all_databases() {
    local backup_file="${BACKUP_DIR}/all_databases_${DATE}.sql"

    print_status "Creating complete backup of all databases..."

    if sudo docker exec "$CONTAINER_NAME" mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --single-transaction --routines --triggers --all-databases > "$backup_file"; then
        # Compress the backup
        gzip "$backup_file"
        print_status "All databases backed up to: ${backup_file}.gz"

        # Get file size
        local file_size=$(du -h "${backup_file}.gz" | cut -f1)
        print_status "Complete backup size: $file_size"
    else
        print_error "Failed to create complete backup"
        return 1
    fi
}

# Function to clean old backups (keep last 7 days)
cleanup_old_backups() {
    print_status "Cleaning up backups older than 7 days..."
    find "$BACKUP_DIR" -name "*.sql.gz" -mtime +7 -delete
    print_status "Cleanup completed"
}

# Function to show backup summary
show_summary() {
    print_status "Backup Summary:"
    echo "=================="
    echo "Backup Directory: $BACKUP_DIR"
    echo "Backup Date: $DATE"
    echo "Available backups:"
    ls -lh "$BACKUP_DIR"/*_${DATE}.sql.gz 2>/dev/null || print_warning "No backups created in this session"
}

# Main execution
main() {
    print_status "Starting MySQL backup process..."

    # Check if container is running
    check_container

    # Create backup directory
    create_backup_dir

    # Get MySQL password
    get_mysql_password

    # Test connection
    test_connection

    # Get databases
    get_databases

    # Ask user for backup type
    echo
    echo "Choose backup type:"
    echo "1) Individual database backups"
    echo "2) Complete backup (all databases in one file)"
    echo "3) Both"
    echo -n "Enter your choice (1-3): "
    read choice

    case $choice in
        1)
            for db in $DATABASES; do
                backup_database "$db"
            done
            ;;
        2)
            backup_all_databases
            ;;
        3)
            for db in $DATABASES; do
                backup_database "$db"
            done
            backup_all_databases
            ;;
        *)
            print_error "Invalid choice. Exiting."
            exit 1
            ;;
    esac

    # Cleanup old backups
    cleanup_old_backups

    # Show summary
    show_summary

    print_status "Backup process completed successfully!"
}

# Run main function
main "$@"