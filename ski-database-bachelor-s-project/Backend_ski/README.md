# Backend (Spring Boot)

REST API pro inventář lyží – PostgreSQL, Flyway, JWT, Argon2.

## Technologie

- **Java 21**, **Spring Boot 3.2.0**, **Maven**
- **PostgreSQL 16**, **Flyway** (migrace)
- **JWT** (auth), **Argon2** (hesla)

## Databázové migrace (Flyway)

`src/main/resources/db/migration/` – spouští se při startu.

| Skript | Popis |
|--------|-------|
| V1__enum_types | Enum typy |
| V2__users / V3__users_data | Uživatelé (Argon2) |
| V4__customers / V5__customers_data | Zákazníci |
| V6__skis / V7__skis_data | Lyže |
| V8–V9 (nahrazeno V19) | Servisní zakázky (refaktor na orders + order_tasks) |
| V19__refactor_orders_structure | orders, order_tasks, migrace service_task_items |
| V10__service_task_items / V11__…_data | Servisní úkoly |
| V12__service_history / V13__…_data | Historie servisů |
| V14__sequences | Sekvence |
| V15–V17 | customer_number (us…), ski_number (sk…), order_number (or…) |

ID formát: zákazníci `us000000001`, lyže `sk000000001`, objednávky `or000000001`.

## Environment proměnné

- `DB_HOST` - host databáze (default: localhost)
- `DB_PORT` - port databáze (default: 5432)
- `DB_NAME` - název databáze (default: ski_inventory)
- `DB_USER` - uživatel databáze (default: postgres)
- `DB_PASSWORD` - heslo databáze (default: postgres)
- `JWT_SECRET` - secret pro JWT (minimálně 32 znaků!)
- `JWT_EXPIRATION` - expirace tokenu v ms (default: 86400000 = 24h)
- `SERVER_PORT` - port aplikace (default: 8080)

## Spuštění

**Lokálně:** Postgres + `createdb ski_inventory`, pak `mvn spring-boot:run` v `Backend_ski/`. API na http://localhost:8080.

**Dokumentace API (Swagger):** Po spuštění je k dispozici na http://localhost:8080/swagger-ui.html (OpenAPI 3: `/v3/api-docs`).

**S Dockerem:** viz **SPUSTENI.md** v kořeni (nebo `docker/README.md`).

Výchozí login: `admin` / `admin123`, `technician` / `tech123`.

## Flyway problém (checksum / validace)

Po změně migrací může být v DB stará historie. Dev: `docker compose -f docker/docker-compose.dev.yml down -v` a znovu `up -d`. Lokálně: `dropdb ski_inventory && createdb ski_inventory`.
r