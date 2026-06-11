#!/bin/sh
# pg_dump do /backups, smazání záloh starších než RETENTION_DAYS

set -e
RETENTION_DAYS="${RETENTION_DAYS:-7}"
BACKUP_DIR="${BACKUP_DIR:-/backups}"
mkdir -p "$BACKUP_DIR"

# Povinné env: PGHOST, PGDATABASE, PGUSER, PGPASSWORD
if [ -z "$PGHOST" ] || [ -z "$PGDATABASE" ] || [ -z "$PGUSER" ]; then
  echo "Chybí PGHOST, PGDATABASE nebo PGUSER. Záloha se neprovede." >&2
  exit 1
fi

TIMESTAMP=$(date +%Y-%m-%d_%H%M%S)
FILE="${BACKUP_DIR}/ski_inventory_${TIMESTAMP}.sql.gz"

echo "[$(date '+%Y-%m-%dT%H:%M:%S')] Záloha startuje: $FILE"
if pg_dump -Fp -h "$PGHOST" -p "${PGPORT:-5432}" -U "$PGUSER" -d "$PGDATABASE" --no-owner --no-acl 2>/dev/null | gzip > "$FILE"; then
  echo "[$(date '+%Y-%m-%dT%H:%M:%S')] Záloha dokončena: $FILE"
else
  echo "[$(date '+%Y-%m-%dT%H:%M:%S')] CHYBA pg_dump" >&2
  rm -f "$FILE"
  exit 1
fi

# Smazání záloh starších než RETENTION_DAYS
echo "[$(date '+%Y-%m-%dT%H:%M:%S')] Mazání záloh starších než ${RETENTION_DAYS} dní"
find "$BACKUP_DIR" -maxdepth 1 -name 'ski_inventory_*.sql.gz' -mtime +"$RETENTION_DAYS" -delete
