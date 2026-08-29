#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend/spring"
FRONTEND_DIR="$PROJECT_DIR/frontend/react-app"
DB_NAME="${DB_NAME:-url_shortener}"
DB_USER="${DB_USERNAME:-url_shortener_app}"
DB_PASSWORD="${DB_PASSWORD:-url_shortener_dev}"

die() { echo "Error: $*" >&2; exit 1; }

command -v npm >/dev/null || die "npm is not installed."

pg_isready >/dev/null || die "PostgreSQL is not running. Start it with: sudo systemctl start postgresql"

if command -v mvn >/dev/null; then
  MVN="$(command -v mvn)"
elif [[ -x /tmp/apache-maven-3.9.11/bin/mvn ]]; then
  MVN="/tmp/apache-maven-3.9.11/bin/mvn"
else
  die "Maven is not installed. Run: sudo apt-get install maven"
fi

echo "Preparing database '$DB_NAME'..."
if ! PGPASSWORD="$DB_PASSWORD" psql -h localhost -U "$DB_USER" -d "$DB_NAME" -c 'select 1' >/dev/null 2>&1; then
  command -v sudo >/dev/null || die "sudo is required for first-time database setup."
  echo "First run: sudo may ask for your Linux password to create a local development role."
  [[ "$DB_USER" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || die "DB_USERNAME must be a simple PostgreSQL role name."
  if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname = '$DB_USER'" | grep -q 1; then
    sudo -u postgres createuser "$DB_USER"
  fi
  escaped_password="${DB_PASSWORD//\'/\'\'}"
  sudo -u postgres psql -v ON_ERROR_STOP=1 -c "ALTER ROLE \"$DB_USER\" WITH LOGIN PASSWORD '$escaped_password';"
  if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" | grep -q 1; then
    sudo -u postgres createdb -O "$DB_USER" "$DB_NAME"
  fi
fi

export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/$DB_NAME}"
export DB_USERNAME="$DB_USER"
export DB_PASSWORD

cleanup() {
  echo
  echo "Stopping URL shortener..."
  kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
  wait "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
}
trap cleanup INT TERM EXIT

echo "Starting Spring Boot API..."
(cd "$BACKEND_DIR" && "$MVN" spring-boot:run) &
BACKEND_PID=$!

echo "Starting React frontend..."
(cd "$FRONTEND_DIR" && npm run dev -- --host 127.0.0.1) &
FRONTEND_PID=$!

echo ""
echo "API:      http://localhost:8080"
echo "Frontend: http://localhost:5173"
echo "Press Ctrl+C to stop both services."
wait -n "$BACKEND_PID" "$FRONTEND_PID"
