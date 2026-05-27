SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'planet_osm_point' /*ORDER BY ordinal_position*/;

SELECT osm_id, name, amenity, shop, tourism, highway, width
FROM planet_osm_point
LIMIT 200;


SELECT amenity, COUNT(*) AS total FROM planet_osm_point WHERE amenity is not null
GROUP BY amenity
ORDER BY total desc;

WITH amenity_info AS (
	SELECT amenity, osm_id, z_order, ST_AsText(way) AS line_string, 
		ROW_NUMBER() over (PARTITION BY amenity ORDER BY osm_id) AS rn, 
		COUNT(*) over (PARTITION BY amenity) AS total_count
	FROM planet_osm_point
	WHERE amenity is not null
)
SELECT amenity, osm_id, z_order, line_string, rn, total_count
FROM amenity_info
WHERE rn <= 20
ORDER BY total_count DESC, rn ASC;






