#!/usr/bin/env python3
import math
import sys

def lonlat_to_xyz(lon, lat, z):
    lat_rad = math.radians(lat)
    n = 2 ** z
    x = int((lon + 180.0) / 360.0 * n)
    y = int((1.0 - math.asinh(math.tan(lat_rad)) / math.pi) / 2.0 * n)
    return x, y

if len(sys.argv) != 4:
    print("Usage:")
    print("  python3 scripts/08_list_tiles_for_bbox.py <min_lon,min_lat,max_lon,max_lat> <minzoom> <maxzoom>")
    sys.exit(1)

bbox = sys.argv[1]
minzoom = int(sys.argv[2])
maxzoom = int(sys.argv[3])

min_lon, min_lat, max_lon, max_lat = map(float, bbox.split(","))

for z in range(minzoom, maxzoom + 1):
    x1, y1 = lonlat_to_xyz(min_lon, max_lat, z)
    x2, y2 = lonlat_to_xyz(max_lon, min_lat, z)

    xmin, xmax = min(x1, x2), max(x1, x2)
    ymin, ymax = min(y1, y2), max(y1, y2)

    for x in range(xmin, xmax + 1):
        for y in range(ymin, ymax + 1):
            print(f"{z}/{x}/{y}")