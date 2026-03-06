#!/usr/bin/env bash
set -euo pipefail

echo "Initializing application databases..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-'EOSQL'
  SELECT 'CREATE DATABASE customer'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'customer')\gexec

  SELECT 'CREATE DATABASE notification'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification')\gexec

  SELECT 'CREATE DATABASE fraud'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'fraud')\gexec
EOSQL

echo "Database initialization completed."

