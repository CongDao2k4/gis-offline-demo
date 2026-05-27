SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'planet_osm_line' ORDER BY ordinal_position;

SELECT osm_id, name, highway, railway, waterway, bridge, tunnel, surface, way
FROM planet_osm_line
WHERE name is not null AND lower(name) LIKE 'đường%trãi%'
LIMIT 100;

SELECT highway, count(*) as total
FROM planet_osm_line
WHERE highway is not null
GROUP BY highway
ORDER BY total desc;

-- Query lấy thuộc tính user-defined cột way ra và encode sang dạng xem được
-- ST_AsText chuyển Geometry sang dạng text LINESTRING()
SELECT osm_id, name, highway, ST_AsText(way) as geometry_text
FROM planet_osm_line
WHERE highway is not null AND name is not null AND lower(name) like '%trãi%'
LIMIT 100;

-- Xem tọa độ gốc web SRID bằng hàm ST_SRID() -> cần đảm bảo có 1 tọa độ gốc thôi.

SELECT distinct ST_SRID(way)
FROM planet_osm_line
LIMIT 1000;

-- chuyển từ vị trí 3857 cửa web mercator sang kinh độ, vĩ độ nhờ hàm ST_Transform(way, 4326)

SELECT name, highway, ST_AsText(ST_Transform(way, 4326)) AS geom_particular
FROM planet_osm_line
WHERE highway is not null
LIMIT 50;

SELECT distinct ST_SRID(geom_particular)
FROM (
	SELECT name, highway, ST_Transform(way, 4326) AS geom_particular
	FROM planet_osm_line
	WHERE highway is not null
	LIMIT 50	
);

-- Tìm các loại highway
SELECT DISTINCT highway FROM planet_osm_line;


-- Tính chiều dài đường, độ dài đường, số điểm, số line con
WITH line_stats AS (
	SELECT name, highway, ROUND(ST_Length(way)::numeric, 2) AS length_m, ST_AsText(way) AS line_string
	FROM planet_osm_line
	WHERE highway is not null AND lower(name) LIKE 'đường%trãi%'
	ORDER BY ST_Length(way) desc
	LIMIT 100
)
SELECT line_stats.* , ST_NPoints(line_stats.line_string) AS num_points, (ST_NPoints(line_stats.line_string) - 1) AS num_sub_lines
FROM line_stats;





















