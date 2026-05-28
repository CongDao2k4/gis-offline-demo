#!/bin/bash
set -e

mkdir -p data

echo "[INFO] Downloading Vietnam OSM PBF..."
# wget -c -O data/vietnam-latest.osm.pbf \
#   https://download.geofabrik.de/asia/vietnam-latest.osm.pbf
wget -c -O data/vietnam-180101.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-180101.osm.pbf

echo "[INFO] Download completed."
ls -lh data/vietnam-180101.osm.pbf

echo "[INFO] Basic file info:"
file data/vietnam-180101.osm.pbf

echo "[INFO] Osmium file info:"
osmium fileinfo data/vietnam-180101.osm.pbf
