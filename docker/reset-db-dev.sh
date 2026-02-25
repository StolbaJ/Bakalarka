#!/bin/bash
# Resetuje dev databázi - smaže volume a nechá Flyway znovu vytvořit schéma

echo "Zastavování kontejnerů..."
docker compose -f docker/docker-compose.dev.yml down

echo "Mazání postgres-data-dev volume..."
docker volume rm postgres-data-dev 2>/dev/null || echo "Volume již neexistuje"

echo "Spouštění kontejnerů s čistou DB..."
docker compose -f docker/docker-compose.dev.yml up -d postgres

echo "Čekání na Postgres..."
sleep 5

echo "Backend nyní spustí Flyway migrace při startu..."
docker compose -f docker/docker-compose.dev.yml up -d backend

echo "Hotovo! DB byla resetována."
