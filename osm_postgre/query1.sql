SELECT name, highway FROM planet_osm_line WHERE highway is not null LIMIT 20;

SELECT highway, COUNT(*)
FROM planet_osm_line
WHERE highway IS NOT NULL
GROUP BY highway
ORDER BY COUNT(*) DESC;

-- Spartial Query đầu tiên:
SELECT name, highway ,ST_AsText(way)
FROM planet_osm_line
WHERE highway IS NOT NULL
LIMIT 5;

-- Query  Spartial

SELECT name FROM planet_osm_line
WHERE ST_Intersects(way, ST_Transform(
							ST_MakeEnvelope(105.8, 21.0, 105.9, 21.1, 4326),
							3857
						)
					);
