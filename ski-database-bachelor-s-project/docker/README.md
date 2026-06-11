# Docker Setup

## Rozdíly mezi dev a prod

### `docker-compose.dev.yml` (vývoj/testování)
- **Profil**: `SPRING_PROFILES_ACTIVE=dev`
- **Volume**: `postgres-data-dev` (oddělený od prod)
- **Porty**: 5432 (Postgres), 8080 (Backend), 3000 (Frontend) - přímo vystavené
- **Flyway**: `clean-disabled: false` (lze smazat DB pomocí Flyway)
- **Bez SSL**: žádný Nginx
- **Logging**: DEBUG úroveň

### `docker-compose.yml` (produkce)
- **Profil**: `SPRING_PROFILES_ACTIVE=prod`
- **Volume**: `postgres-data` (produkční data)
- **Porty**: 80, 443 (přes Nginx) - backend/frontend nejsou přímo vystavené
- **Flyway**: `clean-disabled: true` (ochrana proti smazání DB)
- **SSL**: Nginx reverse proxy + Cloudflare Origin certifikát (cert v repu, klíč přes GitHub Secret)
- **Logging**: INFO úroveň

## Použití

### Dev prostředí
```bash
docker compose -f docker/docker-compose.dev.yml up -d
```

### Produkce
```bash
docker compose -f docker/docker-compose.yml up -d
```

Lokální běh backendu bez Dockeru (Java 21, Postgres, env): **Backend_ski/README.md**.

#### SSL / HTTPS (Cloudflare)
Potřebuješ **doménu** (A záznam na IP serveru). Certifikát: Cloudflare Origin Certificate → ulož jako `docker/ssl/cert.pem` (v repu). Privátní klíč **ne** do repa – jen do GitHub Secret `SSL_PRIVATE_KEY_B64` (base64 klíče). Při deployi (push na `main`) se klíč zapíše na server a Nginx použije `default-ssl.conf`. Detail: **docker/ssl/README.md**.

#### Zálohy
Zálohy (24 h, 7 dní, off-site, restore): **docker/README-BACKUPS.md**.
