#!/usr/bin/env bash
# Bring up a local Postgres in Docker, apply the test schema, and apply
# sql/photos.sql. Idempotent — re-running is safe.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if ! command -v docker >/dev/null 2>&1; then
    echo "docker not found. Install Docker Desktop first." >&2
    exit 1
fi

echo "== Starting Postgres container"
(cd "$SCRIPT_DIR" && docker compose up -d)

echo "== Waiting for Postgres to be ready"
for i in $(seq 1 30); do
    if docker exec csci201_postgres pg_isready -U postgres >/dev/null 2>&1; then
        break
    fi
    sleep 1
    if [ "$i" = "30" ]; then
        echo "Postgres did not become ready" >&2
        exit 1
    fi
done

echo "== Applying scripts/seed.sql"
docker exec -i csci201_postgres psql -U postgres -d postgres \
    -v ON_ERROR_STOP=1 < "$SCRIPT_DIR/seed.sql"

echo "== Applying sql/photos.sql"
docker exec -i csci201_postgres psql -U postgres -d postgres \
    -v ON_ERROR_STOP=1 < "$ROOT/sql/photos.sql"

echo
echo "Postgres ready:"
echo "  DB_URL      = jdbc:postgresql://localhost:5432/postgres"
echo "  DB_USER     = postgres"
echo "  DB_PASSWORD = pg"
echo
echo "Next: ./scripts/run-server.sh"
