#!/bin/sh
# Denní záloha PostgreSQL – pg_dump do /backups, rotace, volitelně off-site (viz README).
set -e

BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETAIN_DAYS="${BACKUP_RETAIN_DAYS:-7}"
PGHOST="${DB_HOST:-postgres}"
PGPORT="${DB_PORT:-5432}"
PGUSER="${DB_USER:-postgres}"
PGPASSWORD="${DB_PASSWORD}"
PGDATABASE="${DB_NAME:-ski_inventory}"

if [ -z "$PGPASSWORD" ]; then
  echo "BACKUP ERROR: DB_PASSWORD not set" >&2
  exit 1
fi

export PGPASSWORD
mkdir -p "$BACKUP_DIR"
STAMP=$(date +%Y%m%d_%H%M%S)
FILE="${BACKUP_DIR}/ski_${STAMP}.sql.gz"

echo "BACKUP: Starting dump of ${PGDATABASE}@${PGHOST} -> ${FILE}"
if pg_dump -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" --no-owner --no-acl | gzip > "$FILE"; then
  echo "BACKUP: Done $(ls -l "$FILE" | awk '{print $5}') bytes"
else
  echo "BACKUP: pg_dump failed" >&2
  exit 1
fi
unset PGPASSWORD

# Rotace – smazat zálohy starší než RETAIN_DAYS
echo "BACKUP: Rotating backups older than ${RETAIN_DAYS} days"
find "$BACKUP_DIR" -name 'ski_*.sql.gz' -mtime +"$RETAIN_DAYS" -delete

# Off-site: pokud je nastaveno BACKUP_OFFSITE_SCRIPT, spustit (např. rclone/aws sync)
if [ -n "$BACKUP_OFFSITE_SCRIPT" ] && [ -x "$BACKUP_OFFSITE_SCRIPT" ]; then
  echo "BACKUP: Running off-site script $BACKUP_OFFSITE_SCRIPT"
  "$BACKUP_OFFSITE_SCRIPT" || echo "BACKUP WARN: off-site script failed" >&2
fi
