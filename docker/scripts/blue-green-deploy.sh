#!/usr/bin/env bash
# Blue-green deploy: staré běží, nabuildí se nové, spustí se nový profil, nginx se přepne, staré se zastaví.
# Volá se z kořene repozitáře. Očekává: .env, docker/ssl/key.pem, docker/nginx/conf.d/default-ssl-{blue,green}.conf.
# Použití: sudo -E ./docker/scripts/blue-green-deploy.sh   (nebo z CI s docker compose přístupem)

set -e
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.yml}"
ENV_FILE="${ENV_FILE:-.env}"
DEPLOY_CURRENT_FILE="${DEPLOY_CURRENT_FILE:-docker/.deploy-current}"
NGINX_CONF_DIR="${NGINX_CONF_DIR:-docker/nginx/conf.d}"

cd "$(dirname "$0")/../.."  # repo root when script is in docker/scripts/

# Nastaví default.conf tak, aby nginx měl platný config (blue, green, nebo blue při prvním deployi)
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
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
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
  # První deploy: spustit blue a nastavit jako aktuální
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --no-cache backend-blue frontend-blue
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --profile blue
  wait_for_healthy backend-blue || true
  cp "$NGINX_CONF_DIR/default-ssl-blue.conf" "$NGINX_CONF_DIR/default.conf"
  reload_nginx
  echo "blue" > "$DEPLOY_CURRENT_FILE"
  echo "First deploy: blue is live."
else
  if [ "$CURRENT" = "blue" ]; then
    NEXT="green"
  else
    NEXT="blue"
  fi
  # Build nových imagí (běží stále starý stack)
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --no-cache backend-$NEXT frontend-$NEXT
  # Spustit nový stack vedle starého
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --profile "$NEXT"
  # Počkat na zdraví nového backendu
  if ! wait_for_healthy "backend-$NEXT"; then
    echo "Warning: backend-$NEXT did not become healthy in time, switching anyway."
  fi
  # Přepnout nginx na nový stack
  cp "$NGINX_CONF_DIR/default-ssl-$NEXT.conf" "$NGINX_CONF_DIR/default.conf"
  reload_nginx
  # Zastavit starý stack
  docker stop backend-$CURRENT frontend-$CURRENT 2>/dev/null || true
  echo "$NEXT" > "$DEPLOY_CURRENT_FILE"
  echo "Switched to $NEXT (was $CURRENT)."
fi

docker image prune -f
