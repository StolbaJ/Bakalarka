# Zálohy a disaster recovery

pg_dump každých 24 h, retention 7 dní. Zálohy do vlastního volume (backup-data), ne do postgres-data.

## Jak to nahodit

### Celý prod stack včetně záloh

Z BP:

```bash
cd docker
docker compose -f docker-compose.yml up -d --build
```

Backup kontejner jede s tím. První záloha hned, pak po 24 h.

### Jen zálohy (postgres už běží)

```bash
cd docker
docker compose -f docker-compose.yml up -d --build backup
```

### Ověření že zálohy běží

```bash
docker logs backup-ski
```

V logu řádky: „Začínám zálohu…“, „Záloha dokončena: …“.

### Výpis záloh

```bash
cd docker
./restore.sh
```

Bez argumentu: výpis obsahu backup volume (soubory ski_inventory_*.sql.gz).

---

## Parametry

| Co | Hodnota |
|----|--------|
| Interval | 24 h (BACKUP_INTERVAL_SEC=86400) |
| Retention | 7 dní (RETENTION_DAYS=7) |
| Umístění | /backups v containeru → volume backup-data |

Změna: v docker-compose.yml u služby backup RETENTION_DAYS / BACKUP_INTERVAL_SEC, pak `docker compose up -d backup`.

---

## Off-site

Zálohy v backup-data jsou na stejném stroji – při výpadku RPi/SD padnou s ním. Doporučení: kopírovat zálohy mimo server.

Možnosti:

1. **Ručně / cron na hostu**  
   Z volume ven:  
   `docker cp backup-ski:/backups/ski_inventory_2025-03-08_020000.sql.gz ~/offsite-backups/`  
   Na hostu cron (denně) – to samé nebo rsync na NAS/jiný server.

2. **Rsync na druhý stroj**  
   `docker cp backup-ski:/backups/. /tmp/backups-export/`  
   `rsync -avz /tmp/backups-export/ user@jiny-server:/zalohy/ski/`

3. **Cloud (S3, B2, …)**  
   Z /tmp/backups-export/ rclone nebo aws s3 cp do bucketu (cron na hostu).

Off-site = mimo RPi/SD, aby při ztrátě stroje zálohy zůstaly.

---

## Testování recovery

Pravidelné ověření, že z zálohy jde DB znovu načíst.

1. **Získat soubor zálohy**  
   `docker cp backup-ski:/backups/ski_inventory_2025-03-08_020000.sql.gz ./test-restore.sql.gz`  
   (datum podle výpisu z ./restore.sh)

2. **Obnovit**  
   Přepíše aktuální DB.  
   `cd docker && ./restore.sh ./test-restore.sql.gz`  
   Potvrzení „yes“ – skript zastaví backend, smaže/vytvoří DB, nahraje zálohu, spustí backend.

3. **Ověření v aplikaci**  
   Kontrola dat (účty, lyže, atd.).

---

## Struktura

- backup/Dockerfile – image s pg_dump
- backup/backup.sh – jedna záloha + smazání starších než 7 dní
- backup/entrypoint.sh – první záloha hned, pak každých 24 h
- restore.sh – výpis záloh / restore z .sql.gz
- docker-compose.yml – služba backup + volume backup-data

---

## Shrnutí (PDF kap. 10)

- Zálohy: služba backup, pg_dump každých 24 h do backup-data.
- Retention 7 dní.
- Off-site: volume na serveru, kopírovat mimo (cron + rsync/rclone/S3).
- Restore: restore.sh; pravidelně testovat obnovu a kontrolovat v aplikaci.
