# Zálohy Postgres – poznámky pro sebe

## Co mám nastavené

- Služba **backup** v docker-compose: každých 24 h pg_dump → gzip → ukládám do volume **backup-data**, soubory `ski_YYYYMMDD_HHMMSS.sql.gz`.
- Rotace: starší než `BACKUP_RETAIN_DAYS` (default 7) mažu.
- Zálohy jsou pořád na stejném stroji (volume), takže při výpadku RPi/SD karty o ně přijdu – proto chci časem doplnit off-site.

## Kde leží zálohy

- V kontejneru: `/backups/`.
- Na hostu: Docker volume `backup-data` (název s prefixem projektu, viz `docker volume ls`).

Když je chci z volume dostat ven:

```bash
# název volume zjistím: docker volume ls
docker run --rm -v <název_volume_backup-data>:/from -v $(pwd):/to alpine cp -v /from/*.sql.gz /to/
```

## Jak obnovit z zálohy

1. Zastavit backend, ať do DB nepisuje:
   ```bash
   docker compose -f docker/docker-compose.yml stop backend
   ```
2. Obnova (soubor nahradit konkrétním zálohovým):
   ```bash
   gunzip -c ski_20250308_030001.sql.gz | docker exec -i postgres-ski psql -U ${DB_USER:-postgres} -d ${DB_NAME:-ski_inventory}
   ```
3. Znovu spustit backend:
   ```bash
   docker compose -f docker/docker-compose.yml start backend
   ```

Občas otestovat restore do testovací DB (nebo dev compose), ať vím že to funguje.

## Off-site (až to budu chtít řešit)

Aby zálohy přežily pád RPi/SD, musím je kopírovat mimo server.

- **Cron + rsync/scp** – na RPi cron (např. po 3:00), rsync z backup volume na NAS/jiný server.
- **S3 / B2** – na hostu rclone nebo aws cli, cron syncuje volume do bucketu.
- **USB disk** – připojit disk, v compose u služby backup dát bind-mount `- /mnt/usb/ski-backups:/backups`. Zálohy pak na jiném médiu než systém; stejně občas zkopírovat i někam off-site.

## Proměnné

- `BACKUP_RETAIN_DAYS` – kolik dní záloh nechávat (default 7).
- DB_* – stejné jako u backendu, berou se z `.env`.

## Logy

```bash
docker logs backup-ski
```

První záloha cca 90 s po startu, pak vždy po 24 h.
