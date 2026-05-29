-- CHÚ Ý: Đảm bảo bạn đã chạy script `11_import_compare_postgis.sh` trước khi chạy các câu query này.
-- Các câu lệnh dưới đây sử dụng biến BBOX tạm thời `ST_MakeEnvelope` để tăng tốc truy vấn.
-- Thay đổi tọa độ BBOX (minLon, minLat, maxLon, maxLat) trong `ST_MakeEnvelope(105.80, 21.00, 105.90, 21.10, 4326)` theo ý muốn.


-- ==============================================================================
-- 1. TRUY VẤN TÌM VẬT THỂ MỚI ĐƯỢC TẠO (CREATIONS)
-- (Có mặt trong vnew nhưng không có trong v2018)
-- ==============================================================================

-- 1.1 Tìm Điểm (Point / POI) mới tạo:
SELECT n.osm_id, n.name, n.tags, n.way
FROM vnew_point n
LEFT JOIN v2018_point o ON n.osm_id = o.osm_id
WHERE o.osm_id IS NULL
  AND ST_Intersects(n.way, ST_Transform(ST_MakeEnvelope(105.80, 21.00, 105.90, 21.10, 4326), 3857));

-- 1.2 Tìm Đường (Line / Roads) mới tạo:
SELECT n.osm_id, n.highway, n.name, n.way
FROM vnew_line n
LEFT JOIN v2018_line o ON n.osm_id = o.osm_id
WHERE o.osm_id IS NULL
  AND ST_Intersects(n.way, ST_Transform(ST_MakeEnvelope(105.80, 21.00, 105.90, 21.10, 4326), 3857));

-- 1.3 Tìm Vùng (Polygon / Buildings/Lakes) mới tạo:
SELECT n.osm_id, n.building, n.landuse, n.way
FROM vnew_polygon n
LEFT JOIN v2018_polygon o ON n.osm_id = o.osm_id
WHERE o.osm_id IS NULL
  AND ST_Intersects(n.way, ST_Transform(ST_MakeEnvelope(105.80, 21.00, 105.90, 21.10, 4326), 3857));


-- ==============================================================================
-- 2. TRUY VẤN TÌM VẬT THỂ BỊ XÓA (DELETIONS)
-- (Có mặt trong v2018 nhưng đã biến mất ở vnew)
-- ==============================================================================

-- 2.1 Tìm Đường (Line) bị xóa:
SELECT o.osm_id, o.highway, o.name, o.way
FROM v2018_line o
LEFT JOIN vnew_line n ON o.osm_id = n.osm_id
WHERE n.osm_id IS NULL
  AND ST_Intersects(o.way, ST_Transform(ST_MakeEnvelope(105.80, 21.00, 105.90, 21.10, 4326), 3857));


-- ==============================================================================
-- 3. TRUY VẤN TÌM VẬT THỂ BỊ THAY ĐỔI (MODIFICATIONS)
-- (Tồn tại ở cả 2 năm nhưng bị dịch chuyển tọa độ hoặc đổi thuộc tính)
-- ==============================================================================

-- 3.1 Tìm Điểm (Point) bị dịch chuyển tọa độ hoặc đổi tên/tags:
SELECT 
    n.osm_id, 
    o.name AS old_name, n.name AS new_name,
    NOT ST_Equals(o.way, n.way) AS is_geometry_changed
FROM vnew_point n
JOIN v2018_point o ON n.osm_id = o.osm_id
WHERE (NOT ST_Equals(o.way, n.way) OR o.tags != n.tags)
  AND ST_Intersects(n.way, ST_Transform(ST_MakeEnvelope(105.80, 21.00, 105.90, 21.10, 4326), 3857));

-- 3.2 Tìm Đường (Line) bị bẻ cong, nắn lại, hoặc đổi loại đường (highway):
SELECT 
    n.osm_id, 
    o.highway AS old_type, n.highway AS new_type,
    o.name AS old_name, n.name AS new_name,
    NOT ST_Equals(o.way, n.way) AS is_geometry_changed
FROM vnew_line n
JOIN v2018_line o ON n.osm_id = o.osm_id
WHERE (NOT ST_Equals(o.way, n.way) OR o.highway != n.highway OR o.name != n.name)
  AND ST_Intersects(n.way, ST_Transform(ST_MakeEnvelope(105.80, 21.00, 105.90, 21.10, 4326), 3857));

-- 3.3 Tìm Vùng (Polygon) bị thay đổi ranh giới (ví dụ: hồ bị lấp một phần):
SELECT 
    n.osm_id,
    o.landuse AS old_landuse, n.landuse AS new_landuse,
    ST_Area(o.way) AS old_area, ST_Area(n.way) AS new_area
FROM vnew_polygon n
JOIN v2018_polygon o ON n.osm_id = o.osm_id
WHERE (NOT ST_Equals(o.way, n.way) OR o.tags != n.tags)
  AND ST_Intersects(n.way, ST_Transform(ST_MakeEnvelope(105.80, 21.00, 105.90, 21.10, 4326), 3857));


-- ==============================================================================
-- 4. TRUY VẤN NÂNG CAO: TÌM CÁC NÚT GIAO THÔNG THAY ĐỔI (NGÃ TƯ -> NGÃ NĂM)
-- Tính số lượng đường kết nối vào một điểm chung và so sánh.
-- Chú ý: Query này nặng, cần giới hạn BBOX rất nhỏ.
-- ==============================================================================
WITH old_intersections AS (
    SELECT ST_Intersection(a.way, b.way) as geom
    FROM v2018_line a, v2018_line b
    WHERE a.osm_id < b.osm_id AND ST_Intersects(a.way, b.way)
      AND ST_Intersects(a.way, ST_Transform(ST_MakeEnvelope(105.80, 21.00, 105.85, 21.05, 4326), 3857))
),
new_intersections AS (
    SELECT ST_Intersection(a.way, b.way) as geom
    FROM vnew_line a, vnew_line b
    WHERE a.osm_id < b.osm_id AND ST_Intersects(a.way, b.way)
      AND ST_Intersects(a.way, ST_Transform(ST_MakeEnvelope(105.80, 21.00, 105.85, 21.05, 4326), 3857))
)
-- So sánh số lượng giao cắt trong cùng 1 vùng
SELECT 'Giao cắt mới' as type, COUNT(*) FROM new_intersections;
