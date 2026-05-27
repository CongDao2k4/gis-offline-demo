SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'planet_osm_polygon' /*ORDER BY ordinal_position*/;

SELECT osm_id, name, building, landuse, planet_osm_polygon.natural, amenity, way
FROM planet_osm_polygon
LIMIT 50;

SELECT building, COUNT(*) AS total
FROM planet_osm_polygon
WHERE building is not null
GROUP BY building
ORDER BY total DESC;

SELECT osm_id, name, COUNT(*) AS total
FROM planet_osm_polygon
WHERE name is not null
GROUP BY name, osm_id
ORDER BY total DESC;

SELECT name, building, ROUND(ST_Area(way)::numeric, 2) AS area_m2
FROM planet_osm_polygon
/*WHERE building IS NOT NULL*/
ORDER BY ST_Area(way) DESC
LIMIT 20;


