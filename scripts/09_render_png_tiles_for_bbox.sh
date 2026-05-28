#!/bin/bash
set -e

if [ "$#" -ne 5 ]; then
  echo "Usage:"
  echo "  sh scripts/09_render_png_tiles_for_bbox.sh <bbox> <minzoom> <maxzoom> <style_id> <output_dir>"
  echo ""
  echo "Example:"
  echo "  sh scripts/09_render_png_tiles_for_bbox.sh \"105.80,21.00,105.90,21.10\" 6 14 hanoi patches/hoan_kiem_patch/png_tiles"
  exit 1
fi

BBOX="$1"
MINZOOM="$2"
MAXZOOM="$3"
STYLE_ID="$4"
OUT_DIR="$5"

mkdir -p "$OUT_DIR"

python3 scripts/08_list_tiles_for_bbox.py "$BBOX" "$MINZOOM" "$MAXZOOM" | while read TILE; do
  Z=$(echo "$TILE" | cut -d/ -f1)
  X=$(echo "$TILE" | cut -d/ -f2)
  Y=$(echo "$TILE" | cut -d/ -f3)

  mkdir -p "$OUT_DIR/$Z/$X"

  URL="http://localhost:8081/styles/${STYLE_ID}/${Z}/${X}/${Y}.png"
  OUT_FILE="$OUT_DIR/$Z/$X/$Y.png"

  echo "[GET] $URL"
  curl -sS "$URL" -o "$OUT_FILE"

  if [ ! -s "$OUT_FILE" ]; then
    echo "[WARN] Empty tile: $TILE"
    rm -f "$OUT_FILE"
  fi
done

echo "[DONE] Rendered PNG tiles:"
find "$OUT_DIR" -type f | wc -l