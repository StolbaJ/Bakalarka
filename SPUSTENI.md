# Spuštění

**Potřeba:** Docker + Docker Compose. Porty 3000, 8080, 5432 volné.

## Dev

**Důležité:** Spouštěj z **kořene repa** (složka BP), ne z `docker/`.

```bash
cd /cesta/k/BP
docker compose -f docker/docker-compose.dev.yml up -d --build
```

- Frontend: http://localhost:3000  
- API: http://localhost:8080  
- DB: localhost:5432

Výchozí login: `admin` / `admin123`, `technician` / `tech123`.

## Prod (Nginx + SSL)

```bash
cd docker
docker compose -f docker/docker-compose.yml up -d
```

SSL: cert v `docker/ssl/cert.pem`, klíč přes GitHub Secret (viz `docker/ssl/README.md`). Deploy: push na `main` → GitHub Actions.

**Email (Resend):** Aby fungovalo posílání emailů, musíš mít lokálně soubor **`.env`** v kořeni repa (s `RESEND_API_KEY=...`) a na produkci v GitHubu **secret `RESEND_API_KEY`** v Actions – při deployi se z něj sestaví `.env` na serveru. Bez toho backend nemá API klíč a emaily nejdou.

## GitHub Actions – jaké secrety musí být nastavené

V repozitáři: **Settings → Secrets and variables → Actions**. Bez těchto secretů deploy na EC2 neproběhne / aplikace na serveru nebude fungovat:

| Secret | Co tam je |
|--------|-----------|
| `EC2_HOST` | IP adresa nebo hostname EC2 serveru (kam se SSH připojuje workflow). |
| `EC2_USERNAME` | Uživatelské jméno pro SSH na EC2 (např. `ubuntu`). |
| `EC2_SSH_KEY` | Celý privátní SSH klíč (.pem), kterým se přistupuje na EC2. Bez uvozovek, včetně řádků `-----BEGIN ... -----` a `-----END ... -----`. |
| `JWT_SECRET` | Tajný řetězec pro podepisování JWT tokenů. Alespoň 32 znaků, v produkci silný náhodný řetězec. |
| `RESEND_API_KEY` | API klíč z Resend (resend.com) pro posílání emailů z backendu. |
| `SSL_PRIVATE_KEY_B64` | Privátní klíč k Cloudflare Origin certifikátu, **zakódovaný v base64** (celý obsah .pem souboru). Na serveru se z něj při deployi vytvoří `docker/ssl/key.pem`. Viz `docker/ssl/README.md`. |

## FE ↔ BE

Frontend volá backend na `NEXT_PUBLIC_API_URL` (dev: při buildu/runu; prod: prázdné = same origin přes Nginx). API base je `/api/`, auth JWT v hlavičce `Authorization: Bearer <token>`.

## Když to nejede

- Rebuild: `docker compose -f docker-compose.dev.yml up -d --build`
- Build bez cache (nový frontend/backend): `docker compose -f docker/docker-compose.dev.yml build --no-cache && docker compose -f docker/docker-compose.dev.yml up -d`
- Reset DB: `docker compose -f docker-compose.dev.yml down -v` pak znovu `up -d`
- Logy: `docker compose -f docker-compose.dev.yml logs -f backend`

Migrace: Flyway při startu backendu, soubory v `Backend_ski/src/main/resources/db/migration/`.

## Zatěžovací testy (JMeter)

`Backend_ski/jmeter/` – smoke / load / long (10 min). Návod: **Backend_ski/jmeter/README-JMETER.md**. Spuštění: `./run-load-test.sh [smoke|load|long]`, pak `open report/index.html`. Výsledky z EC2: stáhnout `report/` nebo `results.jtl` přes `scp`, u sebe `jmeter -g results.jtl -o report`.

