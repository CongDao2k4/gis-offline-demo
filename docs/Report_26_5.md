# Báo cáo công việc thực hiện — Dự án OSM Offline GIS Pipeline

## 1. Các công việc đã hoàn thành

 mục tiêu chính là xây dựng được pipeline xử lý dữ liệu bản đồ OpenStreetMap (OSM) :

* Xử lý dữ liệu `.osm.pbf`
* Thiết lập cơ sở dữ liệu không gian PostgreSQL/PostGIS
* Import dữ liệu OSM vào database bằng `osm2pgsql`
* Tạo vector tiles dạng `.mbtiles`
* Hiểu cách hoạt động của Tilemaker và Lua transformation pipeline
* Chuẩn bị nền tảng cho hệ thống hiển thị bản đồ offline bằng MapLibre GL

---

# 2. Kiến thức tổng quan về OpenStreetMap (OSM)

OpenStreetMap (OSM) là hệ thống dữ liệu bản đồ mã nguồn mở chứa: 

đường sá , sông ngòi , tòa nhà , POI (Point of Interest) , landuse , giao thông

Dữ liệu OSM được lưu dưới dạng Binary `.osm.pbf` nên nhỏ hơn, đọc nhanh hơn.

Đã có sẵn file `hanoi_roads.osm.pbf` chỉ chứa road ways và các node liên quan

---

# 3. Load dữ liệu vào PostgreSQL

## 5.1 Vai trò

PostgreSQL:

* relational database

PostGIS:

* extension GIS cho PostgreSQL

Cho phép lưu:

* Point
* LineString
* Polygon
* spatial index
* spatial query

---

## 5.2 Hệ thống triển khai

Sử dụng:

* Docker Desktop
* PostgreSQL container
* PostGIS extension
* PgAdmin4

---

## 5.3 Kiểm tra PostGIS

```sql
SELECT PostGIS_Version();
```

Kết quả:

```text
3.6 USE_GEOS=1 USE_PROJ=1 USE_STATS=1
```

---

# 6. Công cụ osm2pgsql

## 6.1 Vai trò

`osm2pgsql` dùng để:

```text
OSM PBF
→ PostgreSQL/PostGIS
```

Nó:

* đọc OSM ways/nodes
* xây geometry
* sinh spatial tables

---

## 6.2 Import dữ liệu

```bash
osm2pgsql \
  --create \
  --database gis \
  --username gisuser \
  --host localhost \
  --port 5432 \
  --slim \
  --hstore \
  data/hanoi_roads.osm.pbf
```

---

## 6.3 Ý nghĩa các tham số

| Flag       | Ý nghĩa              |
| ---------- | -------------------- |
| `--create` | Tạo schema mới       |
| `--slim`   | Import tối ưu RAM    |
| `--hstore` | Lưu toàn bộ OSM tags |

---

## 6.4 Các bảng được sinh ra

| Table                | Vai trò                |
| -------------------- | ---------------------- |
| `planet_osm_line`    | roads, rivers          |
| `planet_osm_point`   | POI                    |
| `planet_osm_polygon` | buildings              |
| `planet_osm_roads`   | roads tối ưu rendering |

---

# 7. Spatial Query

## 7.1 Query dữ liệu đường

```sql
SELECT name, highway
FROM planet_osm_line
WHERE highway IS NOT NULL
LIMIT 20;
```

---

## 7.2 Query geometry

```sql
SELECT
    name,
    ST_AsText(way)
FROM planet_osm_line
WHERE highway IS NOT NULL
LIMIT 1;
```

Kết quả:

```text
LINESTRING(...)
```

---

# 8. Vector Tile Pipeline

Đây là phần quan trọng nhất của ngày làm việc.

---

# 9. Công cụ Tilemaker

## 9.1 Vai trò

Tilemaker dùng để:

```text
OSM PBF
→ Vector Tiles
→ MBTiles
```

Không cần PostgreSQL.

---

## 9.2 Kiến thức chính

Tilemaker sử dụng:

| Thành phần    | Vai trò         |
| ------------- | --------------- |
| `config.json` | cấu hình layers |
| `process.lua` | logic transform |
| `.osm.pbf`    | input           |
| `.mbtiles`    | output          |

---

# 10. Lua Transformation Layer

## 10.1 Vai trò của Lua

Lua quyết định:

* object nào được render
* object vào layer nào
* giữ attribute nào

---

## 10.2 Lua Script đã viết

```lua
function way_function()
    local highway = Find("highway")

    if highway ~= "" then
        Layer("roads", false)
        Attribute("class", highway)

        local name = Find("name")
        if name ~= "" then
            Attribute("name", name)
        end
    end
end
```

---

## 10.3 Ý nghĩa

Nếu object có:

* `highway=*`

thì:

* đưa vào layer `roads`
* giữ:

  * class
  * name
  * surface
  * lanes

---

# 11. Sinh MBTiles

## 11.1 Command Docker

```bash
docker run --rm -it \
  -v "$PWD":/data \
  ghcr.io/systemed/tilemaker:master \
  /data/data/hanoi_roads.osm.pbf \
  --output /data/output/hanoi_roads.mbtiles \
  --config /data/tilemaker/config.json \
  --process /data/tilemaker/process.lua
```

---

## 11.2 Kết quả

Sinh file:

```text
output/hanoi_roads.mbtiles
```

Dung lượng:

```text
35 MB
```

---

# 12. MBTiles

## 12.1 MBTiles là gì

MBTiles là:

* SQLite database
* chứa vector tiles dạng:

  * z/x/y

---

## 12.2 Metadata

Tileset chứa:

* layer `roads`
* attributes:

  * class
  * name
  * surface
  * lanes

---

# 13. Kiến thức rút ra

## 13.1 OSM raw không thể render trực tiếp

Phải:

* filter
* transform
* tile hóa

---

## 13.2 Spatial Database và Tile Pipeline khác nhau

| Thành phần | Vai trò          |
| ---------- | ---------------- |
| PostGIS    | lưu/query GIS    |
| Tilemaker  | tạo vector tiles |

---

## 13.3 Lua là semantic transformation layer

Lua biến:

* raw OSM tags

thành:

* rendering layers

---

# 14. Kết quả đạt được

Trong ngày làm việc đã hoàn thành:

* Xử lý dữ liệu OSM
* Extract dữ liệu Hà Nội
* Filter road network
* Thiết lập PostGIS
* Import dữ liệu spatial vào database
* Query spatial data
* Xây dựng vector tile pipeline
* Sinh file `.mbtiles`
* Hiểu kiến trúc offline GIS rendering

---

# 15. Hướng phát triển tiếp theo

Các bước tiếp theo:

* Tile server
* MapLibre GL JS
* Render map offline trên browser
* Bổ sung layers:

  * water
  * buildings
  * POI
* Tối ưu zoom levels
* PMTiles
* Offline mobile GIS pipeline

---

# 16. Kết luận

Sau ngày làm việc, hệ thống đã đạt được pipeline:

```text
OSM PBF
→ Osmium
→ PostGIS
→ Tilemaker + Lua
→ MBTiles
```

Đây là nền tảng của:

* offline map systems
* vector tile infrastructure
* GIS backend engineering
* self-hosted map stack

và tương đồng với kiến trúc của:

* OpenMapTiles
* Mapbox pipeline
* MapTiler
* self-hosted GIS systems.
