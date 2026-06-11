# Backend pro bakalářskou práci

Tento repozitář obsahuje backendovou část bakalářské práce zaměřené na napojení aplikace na strukturovací stroj pro vytahování a zpracování dat (rozsah Industry 4.0) a zároveň integraci s účetním systémem POHODA (Stormware).

Krátké shrnutí funkcionality:

- Modely entit a JPA mapování pro doménu projektu.
- REST API pro komunikaci s účetním systémem POHODA.
- Service pro export a import XML dat směrem k POHODA mServeru.
- Skript v Pythonu pro zasílání dat do POHODY - bude používán na serveru kde běží systém Pohoda (reside v `src/main/resources/` nebo ve `resources` složce ve built aplikaci).

Důležité poznámky ke spuštění

- Aplikace se primárně spouští v Dockeru (viz `docker-compose-dev.yml` v kořenovém adresáři). Před spuštěním se ujistěte, že:
  - je nainstalovaný Docker (a ideálně i `docker compose` nebo `docker-compose` podle vaší distribuce),
  - běží PostgreSQL databáze, ke které se aplikace připojí. Databáze může běžet v samostatném Docker kontejneru (stejném projektu nebo externě) — je nutné zajistit správné připojení (host, port, uživatel, heslo, název DB) v `application.yaml`.

- Příklad spuštění (pokud používáte nový `docker compose`):

  docker compose -f docker-compose-dev.yml up --build

  Pokud máte starší `docker-compose` jako samostatný binární soubor, použijte:

  docker-compose -f docker-compose-dev.yml up --build

  Poznámka: Ujistěte se, že porty (např. 5432 pro Postgres) nejsou obsazené jinými službami.

Flyway (migrace databáze)

- Projekt používá Flyway pro databázové migrace (scripts v `src/main/resources/db/migration` nebo v `classpath:db/migration`).
- Před spuštěním aplikace se ujistěte, že migrace nekolidují s existující schémou (pokud používáte sdílenou databázi s dalším backendem, domluvte se na společném přístupu k migracím — viz dokumentace projektu nebo dočasně vypněte automatické migrace pro vývoj).

Další zdroje

- Oficiální dokumentace Apache Maven: https://maven.apache.org/guides/index.html
- Spring Boot (Maven plugin): https://docs.spring.io/spring-boot/4.0.2/maven-plugin
- Docker Compose (obecné info): https://docs.docker.com/compose/
- Flyway migrace: https://flywaydb.org/documentation/

Kontakt a další poznámky

- Tento soubor je určen pro rychlý start; podrobnější instrukce a specifika nasazení najdete v dokumentaci v repozitáři nebo se obraťte na autora projektu.
