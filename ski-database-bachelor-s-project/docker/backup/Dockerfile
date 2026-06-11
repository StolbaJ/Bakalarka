# Obraz se pg_dump (shodná verze jako postgres:16-alpine)
FROM postgres:16-alpine
RUN apk add --no-cache gzip
COPY backup.sh /backup.sh
RUN chmod +x /backup.sh
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENV BACKUP_DIR=/backups
VOLUME ["/backups"]
ENTRYPOINT ["/entrypoint.sh"]
