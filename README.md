# GIS Offline Map Demo

## 1. Mục tiêu

- Tìm hiểu về file pbf, mbtiles (dữ liệu bản đồ)
- Tìm hiểu về tool osmium (xử lý dữ liệu dạng pbf)
- Tìm hiểu về tool tilemaker (tạo file mbtiles từ file pbf)
- Tìm hiểu về tool mb-util (tạo ảnh từ file mbtiles)

Pipeline dự kiến:

```text
OSM PBF  -> Osmium -> Tilemaker -> MBTiles -> mb-util -> Nginx -> MapLibre GL JS
```

### Hiện tại: cuối ngày 25/5/2026: 

#### Ở giai đoạn hiện tại, project đã hoàn thành phần setup môi trường và kiểm tra dữ liệu đầu vào dạng `.osm.pbf`.

---

## 2. Cách cài đặt sau khi clone project từ GitHub

- Dùng windows thì bật Ubuntu qua WSL : `wsl -d Ubuntu`. Nếu chưa có thì cài qua lệnh `wsl --install`

---

### 2.2. Cài đặt công cụ cần thiết trên WSL Ubuntu

Project hiện đang dùng WSL Ubuntu để chạy các công cụ xử lý bản đồ.

Cài các package cần thiết:

```bash
sudo apt update
sudo apt install -y curl wget git unzip sqlite3 python3 python3-pip nginx osmium-tool

mkdir -p {data,out,public/tiles,nginx,scripts,screenshots}
```

Giải thích các libraries:
- `osmium` chạy được để xử lý file `.osm.pbf`
- `nginx` chạy được để phục vụ tile ở các bước sau
- `python3`, `pip3`, `sqlite3` sẵn sàng cho các bước xử lý MBTiles sau này

---

### 2.4. Tải dữ liệu bản đồ Việt Nam

Tải từ link `https://download.geofabrik.de/asia/vietnam-latest.osm.pbf`

Chạy file tải dữ liệu: `sh scripts/01_download_data.sh`

Dữ liệu lưu ở thư mục `data` trong gốc thư mục project.

---

## 3. Các bước đã hoàn thành

## Bước 1: Setup môi trường và tạo project

Đã hoàn thành cài Ubuntu trên Windows, tạo xong Project và chuẩn bị các thư viện như `python3 python3-pip nginx osmium-tool`

---

## Bước 2: Tải và kiểm tra dữ liệu OpenStreetMap Việt Nam

Đã tải file dữ liệu: `data/vietnam-latest.osm.pbf` nặng khoảng > 300MB

Lệnh kiểm tra metadata và kết quả kiểm tra file .osm.pbf:

```text
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ osmium fileinfo -e data/vietnam-latest.osm.pbf | head -n 40
File:
  Name: data/vietnam-latest.osm.pbf
  Format: PBF
  Compression: none
  Size: 323848065
Header:
  Bounding boxes:
    (102.095945,7.382239,114.642323,23.402135)
  With history: no
  Options:
    generator=osmium/1.16.0
    osmosis_replication_base_url=https://download.geofabrik.de/asia/vietnam-updates
    osmosis_replication_sequence_number=4794
    osmosis_replication_timestamp=2026-05-24T20:21:00Z
    pbf_dense_nodes=true
    pbf_optional_feature_0=Sort.Type_then_ID
    sorting=Type_then_ID
    timestamp=2026-05-24T20:21:00Z
Data:
  Bounding box: (100.6067931,6.2183523,114.6407887,23.5024689)
  Timestamps:
    First: 2007-10-16T12:57:06Z
    Last: 2026-05-24T19:48:55Z
  Objects ordered (by type and id): yes
  Multiple versions of same object: no
  CRC32: not calculated (use --crc/-c to enable)
  Number of changesets: 0
  Number of nodes: 45662234
  Number of ways: 4648533
  Number of relations: 21963
  Smallest changeset ID: 0
  Smallest node ID: 74099711
  Smallest way ID: 9566396
  Smallest relation ID: 13422
  Largest changeset ID: 0
  Largest node ID: 13873884692
  Largest way ID: 1520956961
  Largest relation ID: 20733784
  Number of buffers: 60263 (avg 835 objects per buffer)
  Sum of buffer sizes: 3836918600 (3.659 GB)
```

Lệnh kiểm tra thống kê chi tiết:

```bash
osmium fileinfo -e data/vietnam-latest.osm.pbf | head -n 40
```

```text
Data:
  Bounding box: (100.6067931,6.2183523,114.6407887,23.5024689)
  Timestamps:
    First: 2007-10-16T12:57:06Z
    Last: 2026-05-24T19:48:55Z
  Objects ordered (by type and id): yes
  Multiple versions of same object: no
  Number of nodes: 45662234
  Number of ways: 4648533
  Number of relations: 21963
  Number of buffers: 60263
  Sum of buffer sizes: 3836918600 (3.659 GB)
```

Thống kê dữ liệu:

| Loại đối tượng | Số lượng |
|---|---:|
| Nodes | 45,662,234 |
| Ways | 4,648,533 |
| Relations | 21,963 |

---

## 3. Bước tiếp theo

Bước tiếp theo là dùng Osmium để xử lý dữ liệu đã tải:

```text
data/vietnam-latest.osm.pbf -> osmium extract -> data/hanoi.osm.pbf -> osmium tags-filter -> data/hanoi_roads.osm.pbf
```

Kết quả đã chạy xong và check thông tin chi tiết về 2 file mới sau khi đã chạy file `scripts/02_extract_hanoi.sh`

```bash
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ osmium fileinfo -e data/hanoi.osm.pbf | head -n 40
File:
  Name: data/hanoi.osm.pbf
  Format: PBF
  Compression: none
  Size: 16344071
Header:
  Bounding boxes:
  With history: no
  Options:
    generator=osmium/1.19.0
    pbf_dense_nodes=true
    pbf_optional_feature_0=Sort.Type_then_ID
    sorting=Type_then_ID
Data:
  Bounding box: (105.3261101,20.5420955,106.7530801,21.2748453)
  Timestamps:
    First: 2007-10-16T12:57:06Z
    Last: 2026-05-24T19:25:24Z
  Objects ordered (by type and id): yes
  Multiple versions of same object: no
  CRC32: not calculated (use --crc/-c to enable)
  Number of changesets: 0
  Number of nodes: 1673771
  Number of ways: 354034
  Number of relations: 3150
  Smallest changeset ID: 0
  Smallest node ID: 74126839
  Smallest way ID: 9566396
  Smallest relation ID: 49915
  Largest changeset ID: 0
  Largest node ID: 13873834440
  Largest way ID: 1520955359
  Largest relation ID: 20729843
  Number of buffers: 2768 (avg 733 objects per buffer)
  Sum of buffer sizes: 175033632 (0.166 GB)
  Sum of buffer capacities: 183042048 (0.174 GB, 96% full)
Metadata:
  All objects have following metadata attributes: version+timestamp
  Some objects have following metadata attributes: version+timestamp
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ osmium fileinfo -e data/hanoi_roads.osm.pbf | head -n 40
File:
  Name: data/hanoi_roads.osm.pbf
  Format: PBF
  Compression: none
  Size: 7644068
Header:
  Bounding boxes:
  With history: no
  Options:
    generator=osmium/1.19.0
    pbf_dense_nodes=true
    pbf_optional_feature_0=Sort.Type_then_ID
    sorting=Type_then_ID
Data:
  Bounding box: (105.5963296,20.7791765,106.1389386,21.1886234)
  Timestamps:
    First: 2007-11-04T16:48:49Z
    Last: 2026-05-24T19:25:24Z
  Objects ordered (by type and id): yes
  Multiple versions of same object: no
  CRC32: not calculated (use --crc/-c to enable)
  Number of changesets: 0
  Number of nodes: 646288
  Number of ways: 143730
  Number of relations: 0
  Smallest changeset ID: 0
  Smallest node ID: 75617751
  Smallest way ID: 9566542
  Smallest relation ID: 0
  Largest changeset ID: 0
  Largest node ID: 13873705561
  Largest way ID: 1520518905
  Largest relation ID: 0
  Number of buffers: 1065 (avg 741 objects per buffer)
  Sum of buffer sizes: 66706304 (0.063 GB)
  Sum of buffer capacities: 69795840 (0.066 GB, 96% full)
Metadata:
  All objects have following metadata attributes: version+timestamp
  Some objects have following metadata attributes: version+timestamp
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ ls -lh data/
total 332M
-rwxrwxrwx 1 cong2004 cong2004  16M May 25 09:24 hanoi.osm.pbf
-rwxrwxrwx 1 cong2004 cong2004 7.3M May 25 09:24 hanoi_roads.osm.pbf
-rwxrwxrwx 1 cong2004 cong2004 309M May 24 22:34 vietnam-latest.osm.pbf
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
```

Mục tiêu (ChatGPT đề xuất):

- Cắt riêng khu vực Hà Nội từ dữ liệu Việt Nam.
- Tạo file PBF nhỏ hơn để xử lý nhanh hơn.
- Lọc riêng mạng lưới đường có tag `highway`.
- Chuẩn bị dữ liệu cho bước tạo MBTiles bằng Tilemaker.