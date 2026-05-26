#!/bin/bash
set -e

# =========================
# CONFIG
# =========================

DB_NAME="gis"
DB_USER="gisuser"
DB_PASSWORD="gispassword"
DB_HOST="localhost"
DB_PORT="5432"

INPUT_FILE="data/hanoi_roads.osm.pbf"

export PGPASSWORD=$DB_PASSWORD

echo "[INFO] Checking input file..."

if [ ! -f "$INPUT_FILE" ]; then
    echo "[ERROR] File not found: $INPUT_FILE"
    exit 1
fi

ls -lh "$INPUT_FILE"

# IMPORT OSM -> POSTGIS

echo "[INFO] Starting osm2pgsql import..."

osm2pgsql \
  --create \
  --database "$DB_NAME" \
  --username "$DB_USER" \
  --host "$DB_HOST" \
  --port "$DB_PORT" \
  --slim \
  --hstore \
  "$INPUT_FILE"

echo "[INFO] Import completed."

# TEST QUERY

echo "[INFO] Checking imported tables..."

psql \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  -c "\dt"

echo "[DONE] Step 2 import completed."