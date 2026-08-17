#!/usr/bin/env bash
#
# dev-up.sh — Bootstrap de todo el entorno local de Pedidos en un solo comando.
#
# Levanta (en orden): Postgres -> build backend -> backend -> frontend.
# Todo proceso de larga vida queda DESATADO del shell (setsid + redirección a
# archivo + stdin desde /dev/null) para que este script retorne al instante con
# un veredicto PASS/FAIL y el shell del tool NO quede bloqueado esperando.
#
# Uso:  scripts/dev-up.sh [--backend-port N] [--frontend-port N] [--no-build] [--fresh]
#
# Salida:
#   PASS:  todo arriba, imprime URLs + pids + logs.
#   FAIL:  qué servicio no levantó + log relevante. Exit != 0.

set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT/backend/pedidos"
FRONTEND_DIR="$ROOT/frontend/frontend"
LOG_DIR="/tmp/opencode"
mkdir -p "$LOG_DIR"

BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-3000}"
DO_BUILD=1
FRESH_PG=0

for arg in "$@"; do
  case "$arg" in
    --no-build) DO_BUILD=0 ;;
    --fresh) FRESH_PG=1 ;;
    --backend-port=*) BACKEND_PORT="${arg#*=}" ;;
    --frontend-port=*) FRONTEND_PORT="${arg#*=}" ;;
  esac
done

BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"
BACKEND_PID_FILE="$LOG_DIR/backend.pid"
FRONTEND_PID_FILE="$LOG_DIR/frontend.pid"

PG_CONTAINER="pedidos-pg"

fail() { echo "FAIL: $1"; [ -n "${2:-}" ] && echo "--- log ($2) ---" && tail -30 "$2" 2>/dev/null; exit 1; }

# ---------- 1. Postgres ----------
echo ">> Postgres"
pg_running() { (pg_isready -h localhost -p 5432 -U pedidos -d pedidos >/dev/null 2>&1) || (ss -ltn 2>/dev/null | grep -q ':5432'); }
if pg_running; then
  echo "   Postgres ya responde en :5432"
else
  if command -v podman >/dev/null 2>&1; then
    # ¿Existe el contenedor? ¿Está parado? ¿No existe?
    if podman ps -a --format '{{.Names}}' 2>/dev/null | grep -q "^$PG_CONTAINER$"; then
      podman start "$PG_CONTAINER" >/dev/null 2>&1 && echo "   Contenedor $PG_CONTAINER iniciado" || fail "no se pudo iniciar $PG_CONTAINER"
    else
      podman run -d --name "$PG_CONTAINER" -p 5432:5432 \
        -e POSTGRES_DB=pedidos -e POSTGRES_USER=pedidos -e POSTGRES_PASSWORD=pedidos \
        postgres:16-alpine >/dev/null 2>&1 && echo "   Contenedor $PG_CONTAINER creado" || fail "no se pudo crear $PG_CONTAINER"
    fi
  elif command -v docker >/dev/null 2>&1; then
    docker ps -a --format '{{.Names}}' 2>/dev/null | grep -q "^$PG_CONTAINER$" \
      && docker start "$PG_CONTAINER" >/dev/null 2>&1 \
      || docker run -d --name "$PG_CONTAINER" -p 5432:5432 \
           -e POSTGRES_DB=pedidos -e POSTGRES_USER=pedidos -e POSTGRES_PASSWORD=pedidos \
           postgres:16-alpine >/dev/null 2>&1
    echo "   Postgres (docker) iniciado"
  else
    fail "Postgres no responde y no hay podman/docker disponible para levantarlo"
  fi
  for i in $(seq 1 30); do
    (pg_isready -h localhost -p 5432 -U pedidos -d pedidos >/dev/null 2>&1) && break
    sleep 1
  done
  pg_running || fail "Postgres no quedó listo tras 30s"
fi
[ "$FRESH_PG" = 1 ] && echo "   (--fresh) BD quedará reseteada por el backend/test/reset" 

# ---------- 2. Build backend ----------
if [ "$DO_BUILD" = 1 ]; then
  echo ">> Build backend (mvnw -DskipTests package)"
  ( cd "$BACKEND_DIR" && ./mvnw -q -DskipTests package > "$LOG_DIR/build.log" 2>&1 )
  [ $? -eq 0 ] || fail "build del backend falló" "$LOG_DIR/build.log"
  echo "   Build OK"
else
  echo ">> Build backend (saltado: --no-build)"
fi

# ---------- 3. Backend ----------
echo ">> Backend en :$BACKEND_PORT"
# Matar si hay uno viejo en ese puerto (por pid file o por patrón).
[ -f "$BACKEND_PID_FILE" ] && kill "$(cat "$BACKEND_PID_FILE")" 2>/dev/null
pkill -f "pedidos-0.0.1-SNAPSHOT.jar" 2>/dev/null
sleep 1
nohup setsid bash -c "SPRING_PROFILES_ACTIVE=dev java -jar '$BACKEND_DIR/target/pedidos-0.0.1-SNAPSHOT.jar' --server.port=$BACKEND_PORT" </dev/null >"$BACKEND_LOG" 2>&1 &
disown
echo $! > "$BACKEND_PID_FILE"
# Espera health
ok=0
for i in $(seq 1 90); do
  if curl -sf "http://localhost:$BACKEND_PORT/api/actuator/health" >/dev/null 2>&1; then ok=1; break; fi
  sleep 2
done
[ "$ok" = 1 ] || fail "el backend no quedó healthy en :$BACKEND_PORT" "$BACKEND_LOG"
echo "   Backend UP ($(cat "$BACKEND_PID_FILE"))"

# ---------- 4. Frontend ----------
echo ">> Frontend en :$FRONTEND_PORT"
[ -f "$FRONTEND_PID_FILE" ] && kill "$(cat "$FRONTEND_PID_FILE")" 2>/dev/null
nohup setsid bash -c "API_PROXY_TARGET=http://localhost:$BACKEND_PORT npm run dev -- -p $FRONTEND_PORT" </dev/null >"$FRONTEND_LOG" 2>&1 &
disown
echo $! > "$FRONTEND_PID_FILE"
ok=0
for i in $(seq 1 60); do
  if curl -sf "http://localhost:$FRONTEND_PORT/login" >/dev/null 2>&1; then ok=1; break; fi
  sleep 2
done
[ "$ok" = 1 ] || fail "el frontend no quedó listo en :$FRONTEND_PORT" "$FRONTEND_LOG"
echo "   Frontend UP ($(cat "$FRONTEND_PID_FILE"))"

echo
echo "PASS — entorno arriba"
echo "  Backend : http://localhost:$BACKEND_PORT/api/actuator/health  (log: $BACKEND_LOG, pid: $(cat "$BACKEND_PID_FILE"))"
echo "  Frontend: http://localhost:$FRONTEND_PORT/login                (log: $FRONTEND_LOG, pid: $(cat "$FRONTEND_PID_FILE"))"
echo "  Reset BD dev/test: curl -X POST http://localhost:$BACKEND_PORT/api/test/reset"
