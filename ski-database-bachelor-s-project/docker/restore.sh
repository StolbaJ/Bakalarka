#!/bin/bash
# Restore DB z .sql.gz. Bez argumentu: výpis záloh v backup-data.
# S argumentem: přepsání DB – zastavení backendu, drop/create DB, nahnutí zálohy, start backendu.

set -e
COMPOSE_FILE="$(dirname "$0")/docker-compose.yml"
BACKUP_VOLUME="backup-data"
DB_NAME="${DB_NAME:-ski_inventory}"
DB_USER="${DB_USER:-postgres}"

list_backups() {
  echo "Zálohy v volume ${BACKUP_VOLUME}:"
  VOL=$(docker volume ls -q | grep backup-data | head -1)
  if [ -z "$VOL" ]; then
    echo "Volume s zálohami nenalezeno – spustit stack včetně backup služby."
    exit 1
  fi
  docker run --rm -v "$VOL:/backups:ro" alpine ls -la /backups
}

restore_from() {
  local file="$1"
  if [ ! -f "$file" ]; then
    echo "Soubor neexistuje: $file" >&2
    exit 1
  fi
  if [[ "$file" != *.sql.gz ]] && [[ "$file" != *.sql ]]; then
    echo "Očekávám .sql nebo .sql.gz: $file" >&2
    exit 1
  fi

  echo "Obnovení z: $file"
  echo "Databáze: $DB_NAME, uživatel: $DB_USER"
  read -p "Přepsat aktuální DB? (yes/no): " confirm
  if [ "$confirm" != "yes" ]; then
    echo "Zrušeno."
    exit 0
  fi

  SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
  cd "$SCRIPT_DIR"
  echo "Zastavení backendu..."
  docker compose -f docker-compose.yml stop backend 2>/dev/null || true

  CONTAINER="postgres-ski"
  TMP_IN="/tmp/restore_$$.sql.gz"
  docker cp "$file" "$CONTAINER:$TMP_IN"

  echo "Drop a create databáze..."
  docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();" 2>/dev/null || true
  docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
  docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"

  echo "Obnovování dat..."
  if [[ "$file" == *.gz ]]; then
    docker exec "$CONTAINER" sh -c "gunzip -c $TMP_IN | psql -U $DB_USER -d $DB_NAME"
  else
    docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$file"
  fi
  docker exec "$CONTAINER" rm -f "$TMP_IN"

  echo "Start backendu..."
  docker compose -f docker-compose.yml start backend
  cd - >/dev/null

  echo "Restore dokončen."
}

if [ $# -eq 0 ]; then
  list_backups
  echo ""
  echo "Restore: ./restore.sh /cesta/k/souboru.sql.gz"
  echo "Z volume ven: docker cp backup-ski:/backups/ski_inventory_2025-03-08_020000.sql.gz ."
  exit 0
fi

restore_from "$1"
