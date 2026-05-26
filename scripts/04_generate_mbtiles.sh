#!/bin/bash
set -e

INPUT_FILE="data/hanoi_roads.osm.pbf"
OUTPUT_FILE="output/hanoi_roads.mbtiles"
CONFIG_FILE="tilemaker/config.json"
PROCESS_FILE="tilemaker/process.lua"

echo "[INFO] Checking input files..."

ls -lh "$INPUT_FILE"
ls -lh "$CONFIG_FILE"
ls -lh "$PROCESS_FILE"

mkdir -p output

echo "[INFO] Removing old output if exists..."
rm -f "$OUTPUT_FILE"

echo "[INFO] Generating MBTiles with Tilemaker..."

docker run --rm -it \
  -v "$PWD":/data \
  ghcr.io/systemed/tilemaker:master \
  /data/"$INPUT_FILE" \
  --output /data/"$OUTPUT_FILE" \
  --config /data/"$CONFIG_FILE" \
  --process /data/"$PROCESS_FILE"

echo "[INFO] Generated:"
ls -lh "$OUTPUT_FILE"

echo "[INFO] MBTiles metadata:"
sqlite3 "$OUTPUT_FILE" "SELECT * FROM metadata;"

echo "[DONE] Vector Tile Pipeline completed."