#!/bin/bash
set -e

INPUT="data/vietnam-latest.osm.pbf"
HANOI_OUTPUT="data/hanoi.osm.pbf"
HANOI_ROADS_OUTPUT="data/hanoi_roads.osm.pbf"

# Bounding box Ha Noi approximate: min_lon, min_lat, max_lon, max_latn -> Kinh độ tối thiểu, Vĩ độ tối thiểu, Kinh độ tối đa, Vĩ độ tối đa. Tìm trên Google Earth

HANOI_BBOX="105.17,20.33,106.03,21.23"

echo "[INFO] Input file:"
ls -lh "$INPUT"

echo "[INFO] Extracting Ha Noi area..."
osmium extract \
  --strategy complete_ways \
  -b "$HANOI_BBOX" \
  "$INPUT" \
  -o "$HANOI_OUTPUT" \
  --overwrite

echo "[INFO] Ha Noi extract created:"
ls -lh "$HANOI_OUTPUT"

echo "[INFO] Ha Noi extract fileinfo:"
osmium fileinfo "$HANOI_OUTPUT"

echo "[INFO] Filtering highway ways from Ha Noi extract..."
osmium tags-filter \
  "$HANOI_OUTPUT" \
  w/highway \
  -o "$HANOI_ROADS_OUTPUT" \
  --overwrite

echo "[INFO] Ha Noi roads file created:"
ls -lh "$HANOI_ROADS_OUTPUT"

echo "[INFO] Ha Noi roads fileinfo:"
osmium fileinfo "$HANOI_ROADS_OUTPUT"

echo "[DONE] Step 3 completed."