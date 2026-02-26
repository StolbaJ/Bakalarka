#!/usr/bin/env bash
# Plný reset: smaže kontejnery i volume (DB), pak nasadí znovu. BE i FE vždy build --no-cache.
# Volá se z kořene repozitáře. Očekává: .env, docker/ssl/key.pem.

set -e
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.yml}"
ENV_FILE="${ENV_FILE:-.env}"
DEPLOY_CURRENT_FILE="${DEPLOY_CURRENT_FILE:-docker/.deploy-current}"
NGINX_CONF_DIR="${NGINX_CONF_DIR:-docker/nginx/conf.d}"

cd "$(dirname "$0")/../.."

echo "Full wipe: stopping containers and removing volumes (DB will be empty)..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down -v

rm -f "$DEPLOY_CURRENT_FILE"

echo "Building BE + FE from scratch (--no-cache)..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --no-cache backend-blue frontend-blue

echo "Starting infra (postgres + nginx) and blue stack..."
cp "$NGINX_CONF_DIR/default-ssl-blue.conf" "$NGINX_CONF_DIR/default.conf"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --profile blue

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
