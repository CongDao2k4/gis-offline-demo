# GIS Offline Demo: PBF to MBTiles to MapLibre

## Goal

Build a local/offline map demo from OpenStreetMap Vietnam data.

## Pipeline

OSM PBF -> Osmium -> Tilemaker -> MBTiles -> mb-util -> Nginx -> MapLibre GL JS

## Project Location

This project is stored in:

D:\A-VinVSF\gis-offline-demo

WSL path:

/mnt/d/A-VinVSF/gis-offline-demo

## Tools

- Osmium: inspect, extract, and filter OSM PBF data
- Tilemaker: convert OSM PBF to vector MBTiles
- mb-util: export MBTiles to tile folder
- Nginx: serve static vector tiles
- MapLibre GL JS: render vector tiles in browser
