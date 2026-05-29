#!/bin/bash
set -e

DB_NAME="gis"
DB_USER="gisuser"
DB_PASSWORD="gispassword"
DB_HOST="localhost"
DB_PORT="5432"

export PGPASSWORD=$DB_PASSWORD

FILE_2018="data/vietnam-180101.osm.pbf"
FILE_NEW="data/vietnam-latest.osm.pbf"

BBOX="$1"

if [ -n "$BBOX" ]; then
    echo "[INFO] BBOX provided: $BBOX. Extracting subsets for faster import..."
    
    EXTRACT_2018="data/temp_2018_extract.osm.pbf"
    EXTRACT_NEW="data/temp_new_extract.osm.pbf"

    osmium extract --strategy complete_ways -b "$BBOX" "$FILE_2018" -o "$EXTRACT_2018" --overwrite
    osmium extract --strategy complete_ways -b "$BBOX" "$FILE_NEW" -o "$EXTRACT_NEW" --overwrite

    FILE_2018="$EXTRACT_2018"
    FILE_NEW="$EXTRACT_NEW"
else
    echo "[WARN] No BBOX provided. Importing FULL Vietnam datasets. This may take hours!"
    echo "Usage to test small region: sh scripts/11_import_compare_postgis.sh \"105.80,21.00,105.90,21.10\""
    sleep 3
fi

# DROP OLD PREFIX TABLES
echo "[INFO] Dropping old comparison tables..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" <<EOF
DROP TABLE IF EXISTS v2018_line CASCADE;
DROP TABLE IF EXISTS v2018_point CASCADE;
DROP TABLE IF EXISTS v2018_polygon CASCADE;
DROP TABLE IF EXISTS v2018_roads CASCADE;

DROP TABLE IF EXISTS vnew_line CASCADE;
DROP TABLE IF EXISTS vnew_point CASCADE;
DROP TABLE IF EXISTS vnew_polygon CASCADE;
DROP TABLE IF EXISTS vnew_roads CASCADE;
EOF

echo "[INFO] Importing 2018 data with prefix 'v2018'..."
osm2pgsql \
  --create \
  --database "$DB_NAME" \
  --username "$DB_USER" \
  --host "$DB_HOST" \
  --port "$DB_PORT" \
  --slim \
  --hstore \
  --prefix "v2018" \
  "$FILE_2018"

echo "[INFO] Importing Latest data with prefix 'vnew'..."
osm2pgsql \
  --create \
  --database "$DB_NAME" \
  --username "$DB_USER" \
  --host "$DB_HOST" \
  --port "$DB_PORT" \
  --slim \
  --hstore \
  --prefix "vnew" \
  "$FILE_NEW"

echo "[DONE] Dual import completed successfully. You can now run queries from scripts/12_compare_queries.sql"
