SELECT postgis_full_version();

SELECT name, highway FROM planet_osm_line WHERE highway is not null and name is not null LIMIT 100;
SELECT * FROM planet_osm_line /*WHERE highway is not null and name is not null*/ LIMIT 100;

SELECT name, highway, ST_AsText(way) FROM planet_osm_line WHERE highway is not null LIMIT 20;