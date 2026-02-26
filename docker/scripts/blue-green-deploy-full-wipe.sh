#!/usr/bin/env bash
# Plný reset včetně DB (down -v). Bez --profile – infra s -p bp, app z app-blue compose.

set -e
COMPOSE_INFRA="${COMPOSE_INFRA:-docker/docker-compose.yml}"
COMPOSE_BLUE="${COMPOSE_BLUE:-docker/docker-compose-app-blue.yml}"
ENV_FILE="${ENV_FILE:-.env}"
DEPLOY_CURRENT_FILE="${DEPLOY_CURRENT_FILE:-docker/.deploy-current}"
NGINX_CONF_DIR="${NGINX_CONF_DIR:-docker/nginx/conf.d}"
COMPOSE_PROJECT_INFRA="${COMPOSE_PROJECT_INFRA:-bp}"

cd "$(dirname "$0")/../.."

echo "Full wipe: stopping all and removing volumes (DB will be empty)..."
docker compose -p "$COMPOSE_PROJECT_INFRA" -f "$COMPOSE_INFRA" --env-file "$ENV_FILE" down -v
docker stop backend-blue frontend-blue backend-green frontend-green 2>/dev/null || true

rm -f "$DEPLOY_CURRENT_FILE"

echo "Building BE + FE from scratch (--no-cache)..."
docker compose -f "$COMPOSE_BLUE" --env-file "$ENV_FILE" build --no-cache

echo "Starting infra (postgres + nginx) and blue stack..."
cp "$NGINX_CONF_DIR/default-ssl-blue.conf" "$NGINX_CONF_DIR/default.conf"
docker compose -p "$COMPOSE_PROJECT_INFRA" -f "$COMPOSE_INFRA" --env-file "$ENV_FILE" up -d
docker compose -f "$COMPOSE_BLUE" --env-file "$ENV_FILE" up -d

echo "Waiting for backend to be healthy..."
for i in $(seq 1 60); do
  status=$(docker inspect --format '{{.State.Health.Status}}' backend-blue 2>/dev/null || echo "none")
  if [ "$status" = "healthy" ]; then
    break
  fi
  sleep 2
done

docker exec nginx-proxy nginx -s reload 2>/dev/null || true
echo "blue" > "$DEPLOY_CURRENT_FILE"
echo "Full wipe deploy done. Blue is live, DB is fresh."

docker image prune -f
