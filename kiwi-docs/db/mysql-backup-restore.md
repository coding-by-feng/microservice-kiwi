# MySQL Backup & Restore (Docker Container)

This guide documents operational procedures for backing up and restoring MySQL databases running inside the `kiwi-mysql` Docker container.

Location of scripts:
- Backup script: `kiwi-deploy/backup/mysql_backup.sh`
- Restore script: `kiwi-deploy/backup/mysql_restore.sh`

## 1. Backup Script (`mysql_backup.sh`)

Features:
- Detects running container.
- Prompts for MySQL password (if not embedded).
- Lists non-system databases interactively.
- Supports per-database dumps, full dump, or both.
- Compresses each dump (`.sql.gz`).
- Rotates (deletes) backups older than 7 days.

Run interactively:
```bash
./kiwi-deploy/backup/mysql_backup.sh
```
(From repo root; ensure it is executable: `chmod +x kiwi-deploy/backup/mysql_backup.sh`.)

Generated filenames:
- Per DB: `<db>_YYYYmmdd_HHMMSS.sql.gz`
- Full: `all_databases_YYYYmmdd_HHMMSS.sql.gz`

Backups stored (default): `/home/kason/mysql_backups` (adjust inside script if desired).

### Recommended Cron Entry
Run nightly at 02:00 host time:
```cron
0 2 * * * /absolute/path/to/repo/kiwi-deploy/backup/mysql_backup.sh >> /var/log/mysql_backup.log 2>&1
```

## 2. Restore Script (`mysql_restore.sh`)

Supports interactive selection or non-interactive flags. Works with `.sql.gz` and plain `.sql` dumps.

Basic interactive restore:
```bash
./kiwi-deploy/backup/mysql_restore.sh
```

### Examples
```bash
# Restore a single database to same name
./kiwi-deploy/backup/mysql_restore.sh -f kiwi_app_20250101_010203.sql.gz

# Restore a single database to a different name
./kiwi-deploy/backup/mysql_restore.sh -f kiwi_app_20250101_010203.sql.gz -d kiwi_app_clone

# Restore all databases
./kiwi-deploy/backup/mysql_restore.sh -f all_databases_20250101_010203.sql.gz -a

# Dry-run to preview
./kiwi-deploy/backup/mysql_restore.sh -f all_databases_20250101_010203.sql.gz -a -n
```

### Flags Summary
| Flag | Description | Default |
|------|-------------|---------|
| -c <container> | Docker container name | kiwi-mysql |
| -b <backup_dir> | Directory containing dumps | /home/kason/mysql_backups |
| -f <file> | Backup file (absolute or relative) | (prompt) |
| -d <database> | Target DB name override | inferred |
| -a | Force treat as full/all-databases dump | off |
| -u <user> | MySQL user | root |
| -p <password> | MySQL password (prompts if missing) | (prompt) |
| -n | Dry-run (show commands) | off |
| -y | Auto-confirm actions | off |
| -h | Help | - |

### Behavior Details
- Infers database name from filename pattern `<db>_YYYYmmdd_HHMMSS.sql(.gz)`.
- Creates target database if missing (single-db mode).
- Full dumps: executes without selecting a single target DB.
- Temporary decompressed file streamed into `mysql` via `docker exec`.
- Dry-run prints planned commands without executing.

## 3. Operational Best Practices
- Verify container running: `docker ps --format '{{.Names}}' | grep kiwi-mysql`.
- Keep backup directory permissions strict: `chmod 700 /home/kason/mysql_backups`.
- Periodically perform a restore into a staging database (`*_clone`) to validate backups.
- Monitor backup size growth; prune or archive to cold storage (e.g., S3).

## 4. Security Considerations
- Do not commit passwords to version control; prefer interactive input or environment variables.
- Limit host access to backup directory (use OS-level ACLs if needed).
- Consider enabling binary logs for point-in-time recovery.

## 5. Troubleshooting
| Issue | Cause | Resolution |
|-------|-------|-----------|
| Auth failure | Wrong password | Re-enter / check container env vars |
| Long restore time | Large dataset | Increase resources; optimize innodb settings |
| Character set problems | Dump missing charset | Add `--default-character-set=utf8mb4` to dump/restore |
| Packet too large | `max_allowed_packet` low | Tune MySQL config and restart |

## 6. Future Enhancements
- Incremental backups via binlog archiving.
- Automatic S3 upload (`aws s3 cp`).
- Post-restore validation (row counts, checksums).
- Notification integration (Slack / Email on success/failure).

## 7. Quick Reference Commands
```bash
# List backups
ls -lh /home/kason/mysql_backups

# Dry run restore of a single DB
./kiwi-deploy/backup/mysql_restore.sh -f mydb_20250101_010203.sql.gz -n

# Actual restore into clone DB
./kiwi-deploy/backup/mysql_restore.sh -f mydb_20250101_010203.sql.gz -d mydb_clone -y
```

---
Maintained by Kiwi microservice platform.

