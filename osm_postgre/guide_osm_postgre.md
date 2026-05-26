# Dùng Osmium và cắt file PBF

Mục tiêu: hiểu cách dữ liệu OpenStreetMap vận hành và cách dùng lệnh cắt nhỏ file PBF.

- Video học kiến trúc OSM & Python : `Travis Hathaway: Processing Open Street Map Data with Python and PostgreSQL`
    - Nội dung : PyData hướng dẫn kỹ về các loại dữ liệu Nodes , Ways, Relations và tư duy xử lý .pbf
- Video sâu về Osmium: `The Osmium Framework (Jochen Topf)` - `https://www.youtube.com/watch?v=u2MTFTm7g_A`
    - tác giả hướng dẫn tối ưu RAM và cách đọc dữ liệu nhanh.
- Tài liệu tra cứu lệnh Osmium: `Osmium Tool Manual` (osm code) - trang hướng dẫn chính để copy các lệnh cắt vùng tọa độ (extract) và lọc thuộc tính (tags-filter)

# Bước 1: Cài docker image Postgre và kết nối qua PgAdmin4

- Tải code docker compose từ `kartoza/postgis`
- Code **docker-compose.yml**:

```yml
version: '3.8'
services:
  db:
    image: kartoza/postgis:17-3.5
    container_name: osm_db
    environment:
      - POSTGRES_DB=gis
      - POSTGRES_USER=gisuser
      - POSTGRES_PASS=gispassword
      - ALLOW_IP_RANGE=0.0.0.0/0
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: always

volumes:
  pgdata:
```

### Kết nối qua PgAdmin4
- Thông số kết nối trong pgAdminTrong pgAdmin, bạn tạo một Server mới và điền chính xác tại tab Connection:
    - Host name/address: localhost (hoặc 127.0.0.1)
    - Port: 5432
    - Maintenance database: gis
    - Username: gisuser
    - Password: gispassword

# Bước 2: dùng Osmium và Osm2pgsql trên wsl linux và extension Postgis

- Docker image đã có sẵn extension `postgis` rồi. Cần cài `osmium` và `osm2pgsql` với lệnh `sudo apt install osmium osm2pgsql -y`
- Kiểm tra lại `osmium --version` và `osm2pgsql --version`

### Chạy `osm2pgsql` để dump data `.osm.pbf` into PostgreSQL

- Từ bên trong wsl command, ta cần kết nối: `docker exec -it osm_db psql -h localhost -U gisuser -d gis` sau đó nhập password là `gispassword`

```bash
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ docker exec -it osm_db psql -h localhost -U gisuser -d gis
Password for user gisuser: 
psql (17.7 (Debian 17.7-3.pgdg12+1))
SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384, compression: off, ALPN: postgresql)
Type "help" for help.

gis=# 
```

- Chạy osm2pgsql cũng cần chạy import TCP ra bên ngoài windows:

```bash
export PGPASSWORD=gispassword

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

- Kiểm tra lại tại wsl command: `docker exec -it osm_db psql -h localhost -U gisuser -d gis` để vào dòng lệnh của DB gis trên WSL command

Sau đó chạy 1 lệnh SQL:
```bash
gis=# \dt
                 List of relations
  Schema  |         Name         | Type  |  Owner  
----------+----------------------+-------+---------
 public   | osm2pgsql_properties | table | gisuser
 public   | planet_osm_line      | table | gisuser
 public   | planet_osm_nodes     | table | gisuser
 public   | planet_osm_point     | table | gisuser
 public   | planet_osm_polygon   | table | gisuser
 public   | planet_osm_rels      | table | gisuser
 public   | planet_osm_roads     | table | gisuser
 public   | planet_osm_ways      | table | gisuser
 public   | spatial_ref_sys      | table | gisuser
 topology | layer                | table | gisuser
 topology | topology             | table | gisuser
(11 rows)

gis=# 
SELECT name, highway
FROM planet_osm_line
WHERE highway IS NOT NULL
LIMIT 20;
      name      |   highway   
----------------+-------------
 Đường tỉnh 295 | primary
                | secondary
                | footway
                | footway
                | footway
                | footway
                | tertiary
                | footway
                | service
                | footway
                | footway
                | residential
                | residential
                | residential
                | footway
                | service
                | service
                | service
                | footway
                | footway
(20 rows)

gis=# 
```

- Query thử 1 câu:
```SQL
SELECT name, highway, ST_AsText(way)
FROM planet_osm_line
WHERE highway IS NOT NULL
LIMIT 1; 
```
    - Kết quả là:
```bash
gis=# 
SELECT name, highway, ST_AsText(way)
FROM planet_osm_line
WHERE highway IS NOT NULL
LIMIT 1; 

      name      | highway |     st_astext                                                                                           
                                                                                                                                                                                                                               
                                                                                                                                                                                                                               
     
----------------+---------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

 Đường tỉnh 295 | primary | LINESTRING(11796162.424628306 2422540.326522931,11796181.571580723 2422526.112440623,11796185.990964508 2422522.1707223533,11796197.08951774 2422509.676675734,11796206.618466152 2422497.230416318
4,11796216.882123202 2422483.936100968,11796231.320261158 2422453.8955242136,11796243.49861345 2422406.977382104,11796251.50248484 2422363.714379258,11796269.346999213 2422302.7020204356,11796288.237916801 2422200.254766965
8,11796306.750348119 2422081.7788514337,11796340.379966289 2421856.857166268,11796360.628981663 2421728.911919911,11796398.689115567 2421488.4557269844,11796429.591406211 2421298.0351947076,11796459.480689488 2421113.911021
624,11796550.228338383 2420530.820053792,11796578.525752943 2420355.959827927,11796644.415759543 2419948.8710251483,11796652.063408561 2419901.67293221,11796656.30468116 2419875.434573411,11796679.626114482 2419779.14001220
37,11796705.207333466 2419715.1867653774,11796754.866958309 2419632.4240848166,11796823.328445146 2419557.9380048444,11796896.30950331 2419502.261746765,11797106.35825049 2419385.9775171545,11797282.888698988 2419290.629082
162)
(1 row)

(END)
```
Hoặc có thể tạo thành code 1 file .sh để chạy. 



