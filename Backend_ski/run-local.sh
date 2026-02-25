#!/bin/bash

# Skript pro lokální spuštění backendu
# Použije PostgreSQL z Dockeru nebo lokální instalaci

echo "Spoustim backend lokalne..."

# Zkontrolovat, zda běží PostgreSQL v Dockeru
if docker ps | grep -q postgres; then
    echo "PostgreSQL bezi v Dockeru"
    DB_HOST="localhost"
else
    echo "PostgreSQL v Dockeru nebezi. Spoustim..."
    cd ../docker
    docker compose -f docker-compose.dev.yml up -d postgres
    sleep 5
    cd ../Backend_ski
    DB_HOST="localhost"
fi

# Nastavit environment proměnné
export SPRING_PROFILES_ACTIVE=dev
export DB_HOST=${DB_HOST:-localhost}
export DB_PORT=${DB_PORT:-5432}
export DB_NAME=${DB_NAME:-ski_inventory}
export DB_USER=${DB_USER:-postgres}
export DB_PASSWORD=${DB_PASSWORD:-postgres}
export JWT_SECRET=${JWT_SECRET:-dev-secret-key-minimum-32-characters-long-for-testing}
export SERVER_PORT=${SERVER_PORT:-8080}
export SHOW_SQL=true
export LOG_LEVEL=DEBUG

echo "Konfigurace:"
echo "   DB_HOST: $DB_HOST"
echo "   DB_PORT: $DB_PORT"
echo "   DB_NAME: $DB_NAME"
echo "   Server port: $SERVER_PORT"
echo ""

# Spustit aplikaci
echo "Spoustim Spring Boot aplikaci..."
mvn clean compile spring-boot:run
