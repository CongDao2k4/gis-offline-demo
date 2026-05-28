SELECT COUNT(*) FROM planet_osm_polygon;

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

SELECT *, ROUND(ST_Area(way)::numeric, 2) AS area_m2 FROM planet_osm_polygon WHERE amenity LIKE 'university';

SELECT name, building, amenity, ROUND(ST_Area(way)::numeric, 2) AS area_m2, ROW_NUMBER() over (PARTITION BY amenity ORDER BY osm_id) AS rn  
FROM planet_osm_polygon
--WHERE lower(name) not like 'xã%' OR lower(name) not like 'quận%' OR lower(name) not like 'tỉnh%' 
--		OR lower(name) not like 'phường%' OR lower(name) not like '%huyện%' OR lower(name) not like 'thành_phố%' 
--		OR lower(name) not like 'trung_tâm' OR lower(name) not like 'thị_xã%' OR lower(name) not like 'thị_trấn%'    
ORDER BY ST_Area(way) DESC
OFFSET 1000 LIMIT 100;


