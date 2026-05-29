#!/bin/bash
set -e

mkdir -p data

echo "[INFO] Downloading Vietnam OSM PBF..."
wget -c -O data/vietnam-260101.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-260101.osm.pbf
echo "[INFO] Download completed."
ls -lh data/vietnam-260101.osm.pbf


wget -c -O data/vietnam-260301.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-260301.osm.pbf
echo "[INFO] Download completed."
ls -lh data/vietnam-260301.osm.pbf


wget -c -O data/vietnam-260401.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-260401.osm.pbf
echo "[INFO] Download completed."
ls -lh data/vietnam-260401.osm.pbf


wget -c -O data/vietnam-260501.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-260501.osm.pbf
echo "[INFO] Download completed."
ls -lh data/vietnam-260501.osm.pbf


wget -c -O data/vietnam-260523.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-260523.osm.pbf
echo "[INFO] Download completed."
ls -lh data/vietnam-260523.osm.pbf


wget -c -O data/vietnam-260524.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-260524.osm.pbf
echo "[INFO] Download completed."
ls -lh data/vietnam-260524.osm.pbf


wget -c -O data/vietnam-260525.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-260525.osm.pbf
echo "[INFO] Download completed."
ls -lh data/vietnam-260525.osm.pbf


wget -c -O data/vietnam-260526.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-260526.osm.pbf
echo "[INFO] Download completed."
ls -lh data/vietnam-260526.osm.pbf


wget -c -O data/vietnam-260527.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-260527.osm.pbf
echo "[INFO] Download completed."
ls -lh data/vietnam-260527.osm.pbf


wget -c -O data/vietnam-260528.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-260528.osm.pbf
echo "[INFO] Download completed."
ls -lh data/vietnam-260528.osm.pbf

#echo "[INFO] Basic file info:"
#file data/vietnam-180101.osm.pbf
#
#echo "[INFO] Osmium file info:"
#osmium fileinfo data/vietnam-180101.osm.pbf
