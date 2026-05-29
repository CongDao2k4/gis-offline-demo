SELECT osm_id, name, highway, ST_AsText(way)
FROM planet_osm_line
WHERE highway is not null
	AND ST_Intersects( way, ST_Transform(
							ST_MakeEnvelope(105.80, 21.00, 105.90, 21.10, 4326),
							3857
							)
					)
	AND lower(name) LIKE '%cự%lộc%'
LIMIT 200;

-- Lọc các line, road chưa có thật, vẫn đang dự án nhưng map đã lưu vào

SELECT osm_id, name, highway, bridge, construction, tags
FROM planet_osm_line
WHERE
    highway IN ('construction', 'proposed')
    OR tags ? 'proposed'
    OR tags ? 'construction'
    OR construction IS NOT NULL
LIMIT 1000;


