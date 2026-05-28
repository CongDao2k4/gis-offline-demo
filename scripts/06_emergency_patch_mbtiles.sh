#!/bin/bash
set -e

if [ "$#" -ne 5 ]; then
  echo "Usage:"
  echo "  sh scripts/06_emergency_patch_mbtiles.sh <input.pbf> <bbox> <patch_name> <minzoom> <maxzoom>"
  echo ""
  echo "Example:"
  echo "  sh scripts/06_emergency_patch_mbtiles.sh data/vietnam-latest.osm.pbf \"105.80,21.00,105.90,21.10\" hoan_kiem_patch 6 14"
  exit 1
fi

INPUT_PBF="$1"
BBOX="$2"
PATCH_NAME="$3"
MINZOOM="$4"
MAXZOOM="$5"

PATCH_DIR="patches/${PATCH_NAME}"
PATCH_PBF="${PATCH_DIR}/${PATCH_NAME}.osm.pbf"
PATCH_MBTILES="${PATCH_DIR}/${PATCH_NAME}.mbtiles"

CONFIG_FILE="tilemaker/config.json"
PROCESS_FILE="tilemaker/process.lua"

mkdir -p "$PATCH_DIR"

echo "[INFO] Input PBF: $INPUT_PBF"
echo "[INFO] BBOX: $BBOX"
echo "[INFO] Patch name: $PATCH_NAME"

if [ ! -f "$INPUT_PBF" ]; then
  echo "[ERROR] Input PBF not found: $INPUT_PBF"
  exit 1
fi

echo "[INFO] Patch zoom range: ${MINZOOM}-${MAXZOOM}"
echo "[1/3] Extract bbox with osmium..."

osmium extract \
  --strategy complete_ways \
  -b "$BBOX" \
  "$INPUT_PBF" \
  -o "$PATCH_PBF" \
  --overwrite

echo "[2/3] Generate patch MBTiles..."

rm -f "$PATCH_MBTILES"

docker run --rm -it \
  -v "$PWD":/data \
  ghcr.io/systemed/tilemaker:master \
  /data/"$PATCH_PBF" \
  --output /data/"$PATCH_MBTILES" \
  --config /data/"$CONFIG_FILE" \
  --process /data/"$PROCESS_FILE"

echo "[3/3] Patch generated:"
ls -lh "$PATCH_PBF"
ls -lh "$PATCH_MBTILES"

echo "[DONE] $PATCH_MBTILES"