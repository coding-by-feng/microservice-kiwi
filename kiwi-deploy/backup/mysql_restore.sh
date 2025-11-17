#!/bin/bash

# MySQL Container Restore Script
# Restores backups created by mysql_backup.sh (individual databases or all_databases)
# Supports interactive selection or non-interactive flags.
#
# Usage examples:
#   ./mysql_restore.sh                          # interactive mode
#   ./mysql_restore.sh -f /path/kiwi_20250101_010203.sql.gz -d kiwi_test
#   ./mysql_restore.sh -f all_databases_20250101_010203.sql.gz -a
#   ./mysql_restore.sh -f kiwi_20250101_010203.sql.gz -n   # dry run
#   ./mysql_restore.sh -f kiwi_20250101_010203.sql -d kiwi_test  # uncompressed
#
# Flags:
#   -c <container>    Docker container name (default: kiwi-mysql)
#   -b <backup_dir>   Backup directory (default: $HOME/mysql_backups)
#   -f <file>         Backup file to restore (absolute or relative)
#   -d <database>     Target database name (override for single DB restore)
#   -a                Treat file as all-databases backup (override detection)
#   -u <user>         MySQL user (default: root)
#   -p <password>     MySQL password (if omitted will prompt)
#   -n                Dry-run (show commands without executing)
#   -y                Assume yes to confirmation prompts
#   -h                Show help

set -o pipefail

CONTAINER_NAME="kiwi-mysql"
# Default backup directory now uses current user's HOME for portability
BACKUP_DIR="$HOME/mysql_backups"
MYSQL_USER="root"
MYSQL_PASSWORD=""
DRY_RUN=false
ASSUME_YES=false
FORCE_ALL=false
BACKUP_FILE=""
OVERRIDE_DB=""

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_step() { echo -e "${BLUE}==>${NC} $1"; }

show_help() {
  sed -n '1,120p' "$0" | grep -E '^#' | sed 's/^# \{0,1\}//'
}

# Parse flags (note: no spaces inside getopts option string)
while getopts ":c:b:f:d:u:p:anyh" opt; do
  case $opt in
    c) CONTAINER_NAME="$OPTARG" ;;
    b) BACKUP_DIR="$OPTARG" ;;
    f) BACKUP_FILE="$OPTARG" ;;
    d) OVERRIDE_DB="$OPTARG" ;;
    u) MYSQL_USER="$OPTARG" ;;
    p) MYSQL_PASSWORD="$OPTARG" ;;
    a) FORCE_ALL=true ;;
    n) DRY_RUN=true ;;
    y) ASSUME_YES=true ;;
    h) show_help; exit 0 ;;
    :) print_error "Option -$OPTARG requires an argument"; exit 1 ;;
    \?) print_error "Invalid option: -$OPTARG"; show_help; exit 1 ;;
  esac
done

# Ensure backup directory exists (create if missing)
ensure_backup_dir() {
  if [ ! -d "$BACKUP_DIR" ]; then
    print_step "Creating backup directory '$BACKUP_DIR'"
    if $DRY_RUN; then
      print_info "(dry-run) mkdir -p '$BACKUP_DIR'"
    else
      if ! mkdir -p "$BACKUP_DIR"; then
        print_error "Failed to create directory '$BACKUP_DIR'"; exit 1
      fi
      chmod 700 "$BACKUP_DIR" 2>/dev/null || true
    fi
  fi
}

check_container() {
  if ! sudo docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    print_error "Container '${CONTAINER_NAME}' is not running."; exit 1
  fi
  print_info "Container '${CONTAINER_NAME}' is running."
}

prompt_password() {
  if [ -z "$MYSQL_PASSWORD" ]; then
    read -s -p "Enter MySQL password for user '$MYSQL_USER': " MYSQL_PASSWORD
    echo
  fi
}

test_connection() {
  print_step "Testing MySQL connection..."
  if ! sudo docker exec "$CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" &>/dev/null; then
    print_error "MySQL connection failed. Check credentials."; exit 1
  fi
  print_info "MySQL connection successful."
}

list_backups() {
  print_step "Listing available backups in $BACKUP_DIR"
  local files=("$BACKUP_DIR"/*.sql.gz "$BACKUP_DIR"/*.sql)
  files=("${files[@]}")
  local present=()
  for f in "${files[@]}"; do
    [ -f "$f" ] && present+=("$f")
  done
  if [ ${#present[@]} -eq 0 ]; then
    print_error "No backup files found in $BACKUP_DIR"; exit 1
  fi
  local i=1
  for f in "${present[@]}"; do
    echo "  $i) $(basename "$f")"
    i=$((i+1))
  done
  echo
  read -p "Select a file number to restore: " choice
  if ! [[ $choice =~ ^[0-9]+$ ]]; then
    print_error "Invalid selection."; exit 1
  fi
  local idx=$((choice-1))
  BACKUP_FILE="${present[$idx]}"
}

resolve_backup_file() {
  ensure_backup_dir
  if [ -z "$BACKUP_FILE" ]; then
    list_backups
  else
    if [ ! -f "$BACKUP_FILE" ]; then
      if [ -f "$BACKUP_DIR/$BACKUP_FILE" ]; then
        BACKUP_FILE="$BACKUP_DIR/$BACKUP_FILE"
      else
        print_error "Backup file '$BACKUP_FILE' not found."; exit 1
      fi
    fi
  fi
  print_info "Selected backup file: $(basename "$BACKUP_FILE")"
}

confirm_action() {
  if $ASSUME_YES; then return 0; fi
  read -p "Proceed with restore? (y/N): " ans
  [[ "$ans" =~ ^[Yy]$ ]] || { print_warn "Restore cancelled."; exit 0; }
}

detect_type_and_db() {
  local base
  base=$(basename "$BACKUP_FILE")
  if $FORCE_ALL || [[ $base == all_databases_* ]]; then
    RESTORE_ALL=true
    SOURCE_DB="(all)"
  else
    RESTORE_ALL=false
    SOURCE_DB=$(echo "$base" | sed -E 's/\.sql(\.gz)?$//' | sed -E 's/_[0-9]{8}_[0-9]{6}$//')
  fi
  TARGET_DB="$SOURCE_DB"
  if ! $RESTORE_ALL; then
    if [ -n "$OVERRIDE_DB" ]; then TARGET_DB="$OVERRIDE_DB"; fi
    print_info "Source database name inferred: $SOURCE_DB"
    print_info "Target database name: $TARGET_DB"
  else
    print_info "Detected an ALL DATABASES backup."
  fi
}

prepare_temp_file() {
  TMP_SQL=$(mktemp /tmp/mysql_restore_XXXXXX.sql)
  print_step "Preparing SQL from backup file"
  case "$BACKUP_FILE" in
    *.sql.gz)
      if $DRY_RUN; then
        print_info "(dry-run) gunzip -c '$BACKUP_FILE' > '$TMP_SQL'"
      else
        if ! gunzip -c "$BACKUP_FILE" > "$TMP_SQL"; then
          print_error "Failed to decompress backup file."; rm -f "$TMP_SQL"; exit 1
        fi
      fi
      ;;
    *.sql)
      if $DRY_RUN; then
        print_info "(dry-run) cp '$BACKUP_FILE' '$TMP_SQL'"
      else
        cp "$BACKUP_FILE" "$TMP_SQL" || { print_error "Failed to copy .sql file"; rm -f "$TMP_SQL"; exit 1; }
      fi
      ;;
    *)
      print_error "Unsupported backup file extension (expected .sql or .sql.gz)"; rm -f "$TMP_SQL"; exit 1
      ;;
  esac
}

create_database_if_needed() {
  if $RESTORE_ALL; then return 0; fi
  print_step "Ensuring database '$TARGET_DB' exists"
  local cmd="CREATE DATABASE IF NOT EXISTS \`$TARGET_DB\`;"
  if $DRY_RUN; then
    print_info "(dry-run) mysql -u$MYSQL_USER -p*** -e '$cmd'"
  else
    if ! sudo docker exec "$CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "$cmd"; then
      print_error "Failed to create or access database '$TARGET_DB'"; exit 1
    fi
  fi
}

perform_restore() {
  local exec_cmd
  if $RESTORE_ALL; then
    print_step "Restoring ALL databases from backup"
    exec_cmd=(sudo docker exec -i "$CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD")
  else
    print_step "Restoring database '$TARGET_DB'"
    exec_cmd=(sudo docker exec -i "$CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$TARGET_DB")
  fi
  if $DRY_RUN; then
    print_info "(dry-run) ${exec_cmd[*]} < $TMP_SQL"
  else
    if ! "${exec_cmd[@]}" < "$TMP_SQL"; then
      print_error "Restore failed."; exit 1
    fi
    print_info "Restore completed successfully."
  fi
}

cleanup() {
  if [ -n "${TMP_SQL:-}" ] && [ -f "$TMP_SQL" ]; then
    rm -f "$TMP_SQL"
    print_info "Temporary file removed."
  fi
}

show_summary() {
  echo "----------------------------"
  echo "Restore Summary"
  echo "Container: $CONTAINER_NAME"
  echo "Backup File: $(basename "$BACKUP_FILE")"
  if $RESTORE_ALL; then
    echo "Mode: ALL DATABASES"
  else
    echo "Source DB: $SOURCE_DB"
    echo "Target DB: $TARGET_DB"
  fi
  echo "Dry Run: $DRY_RUN"
  echo "Backup Dir: $BACKUP_DIR"
  echo "----------------------------"
}

main() {
  print_step "Starting MySQL restore process"
  ensure_backup_dir
  check_container
  prompt_password
  test_connection
  resolve_backup_file
  detect_type_and_db
  show_summary
  confirm_action
  prepare_temp_file
  create_database_if_needed
  perform_restore
  cleanup
  print_step "Restore script finished"
}

trap cleanup EXIT
main "$@"
