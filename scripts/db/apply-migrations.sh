#!/usr/bin/env bash
#
# Applies the Users/Reviews migrations (V40-V59) to the configured database without Flyway.
#
# The shared Supabase schema was created outside Flyway and has no schema history table, so Flyway
# refuses to run. This script applies the very same files from src/main/resources/db/migration and
# records them in manual_migration_history, which makes re-runs safe.
#
# Configuration is read from .env at the project root, with environment variables taking precedence.
# Recognised keys: spring.datasource.url (or SPRING_DATASOURCE_URL), DB_USER, DB_PASSWORD.
#
# Usage:
#   ./scripts/db/apply-migrations.sh              apply pending migrations
#   ./scripts/db/apply-migrations.sh --dry-run    show what would run, write nothing
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"
MIGRATIONS_DIR="$PROJECT_ROOT/src/main/resources/db/migration"
# Reserved migration range for the Users/Reviews subgroup (see the requirements doc, section 8).
PATTERN="^V(4[0-9]|5[0-9])__.*\.sql$"

# Reads a key from .env without sourcing it: keys such as spring.datasource.url contain dots and
# are not valid shell identifiers.
read_env_key() {
    local key="$1"
    [ -f "$ENV_FILE" ] || return 0
    sed -n "s/^[[:space:]]*${key}[[:space:]]*=[[:space:]]*\(.*\)$/\1/p" "$ENV_FILE" | tail -n 1
}

JDBC_URL="${SPRING_DATASOURCE_URL:-$(read_env_key 'spring\.datasource\.url')}"
[ -n "$JDBC_URL" ] || JDBC_URL="$(read_env_key 'SPRING_DATASOURCE_URL')"
DB_USER="${DB_USER:-$(read_env_key 'DB_USER')}"
DB_PASSWORD="${DB_PASSWORD:-$(read_env_key 'DB_PASSWORD')}"

if [ -z "$JDBC_URL" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
    echo "ERROR: missing database configuration." >&2
    echo "Set them in $ENV_FILE (or export them) as:" >&2
    echo "  spring.datasource.url=jdbc:postgresql://HOST:PORT/postgres?sslmode=require" >&2
    echo "  DB_USER=..." >&2
    echo "  DB_PASSWORD=..." >&2
    exit 2
fi

DRIVER_JAR="$(find "${HOME}/.m2/repository/org/postgresql/postgresql" -name 'postgresql-*.jar' \
    ! -name '*sources*' ! -name '*javadoc*' 2>/dev/null | sort -V | tail -n 1)"

if [ -z "$DRIVER_JAR" ]; then
    echo "ERROR: PostgreSQL JDBC driver not found in the local Maven repository." >&2
    echo "Run './mvnw dependency:resolve' first to download it." >&2
    exit 2
fi

echo "Database : ${JDBC_URL%%\?*}"
echo "User     : $DB_USER"
echo "Driver   : $(basename "$DRIVER_JAR")"
echo "Source   : ${MIGRATIONS_DIR#"$PROJECT_ROOT"/} (V40-V59)"
echo

JDBC_URL="$JDBC_URL" DB_USER="$DB_USER" DB_PASSWORD="$DB_PASSWORD" \
    java -cp "$DRIVER_JAR" "$PROJECT_ROOT/scripts/db/MigrationRunner.java" \
        "$MIGRATIONS_DIR" "$PATTERN" "$@"
