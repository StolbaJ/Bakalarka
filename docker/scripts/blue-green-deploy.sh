#!/usr/bin/env bash
# Blue-green deploy bez --profile (kompatibilní se starším Docker Compose).
# Infra: -p bp (síť bp_app-network). Aplikace: samostatné compose soubory app-blue / app-green.

set -e
COMPOSE_INFRA="${COMPOSE_INFRA:-docker/docker-compose.yml}"
COMPOSE_BLUE="${COMPOSE_BLUE:-docker/docker-compose-app-blue.yml}"
COMPOSE_GREEN="${COMPOSE_GREEN:-docker/docker-compose-app-green.yml}"
ENV_FILE="${ENV_FILE:-.env}"
DEPLOY_CURRENT_FILE="${DEPLOY_CURRENT_FILE:-docker/.deploy-current}"
NGINX_CONF_DIR="${NGINX_CONF_DIR:-docker/nginx/conf.d}"
COMPOSE_PROJECT_INFRA="${COMPOSE_PROJECT_INFRA:-bp}"

cd "$(dirname "$0")/../.."

set_nginx_default() {
  local cur
  cur=$(get_current)
  if [ -z "$cur" ]; then
    cp "$NGINX_CONF_DIR/default-ssl-blue.conf" "$NGINX_CONF_DIR/default.conf"
  else
    cp "$NGINX_CONF_DIR/default-ssl-$cur.conf" "$NGINX_CONF_DIR/default.conf"
  fi
}

reload_nginx() {
  docker exec nginx-proxy nginx -s reload 2>/dev/null || true
}

ensure_infra_up() {
  docker compose -p "$COMPOSE_PROJECT_INFRA" -f "$COMPOSE_INFRA" --env-file "$ENV_FILE" up -d
}

get_current() {
  if [ -f "$DEPLOY_CURRENT_FILE" ]; then
    cat "$DEPLOY_CURRENT_FILE"
  else
    echo ""
  fi
}

wait_for_healthy() {
  local name=$1
  local max=60
  local i=0
  while [ $i -lt $max ]; do
    status=$(docker inspect --format '{{.State.Health.Status}}' "$name" 2>/dev/null || echo "none")
    if [ "$status" = "healthy" ]; then
      return 0
    fi
    i=$((i+1))
    sleep 2
  done
  return 1
}

# --- main ---
CURRENT=$(get_current)
set_nginx_default
ensure_infra_up

CURRENT=$(get_current)
if [ -z "$CURRENT" ]; then
  # První deploy: blue
  docker compose -f "$COMPOSE_BLUE" --env-file "$ENV_FILE" build --no-cache
  docker compose -f "$COMPOSE_BLUE" --env-file "$ENV_FILE" up -d
  wait_for_healthy backend-blue || true
  cp "$NGINX_CONF_DIR/default-ssl-blue.conf" "$NGINX_CONF_DIR/default.conf"
  reload_nginx
  echo "blue" > "$DEPLOY_CURRENT_FILE"
  echo "First deploy: blue is live."
else
  if [ "$CURRENT" = "blue" ]; then
    NEXT="green"
    COMPOSE_NEXT="$COMPOSE_GREEN"
  else
    NEXT="blue"
    COMPOSE_NEXT="$COMPOSE_BLUE"
  fi
  docker compose -f "$COMPOSE_NEXT" --env-file "$ENV_FILE" build --no-cache
  docker compose -f "$COMPOSE_NEXT" --env-file "$ENV_FILE" up -d
  if ! wait_for_healthy "backend-$NEXT"; then
    echo "Warning: backend-$NEXT did not become healthy in time, switching anyway."
  fi
  cp "$NGINX_CONF_DIR/default-ssl-$NEXT.conf" "$NGINX_CONF_DIR/default.conf"
  reload_nginx
  docker stop backend-$CURRENT frontend-$CURRENT 2>/dev/null || true
  echo "$NEXT" > "$DEPLOY_CURRENT_FILE"
  echo "Switched to $NEXT (was $CURRENT)."
fi

docker image prune -f
