# Chạy wsl hệ điều hành Ubuntu

Liệt kê các phiên bản wsl:
`wsl -l -v`

Kết quả:
```bash
(.venv) PS D:\A-VinVSF\gis-offline-demo> wsl -l -v
  NAME              STATE           VERSION
* docker-desktop    Running         2
  Ubuntu            Running         2
```

Mở Ubuntu: `wsl -d Ubuntu`

Nếu chưa có Ubuntu thì cài ở PowerShell bằng quyền Run as Administartor: `wsl --install`


## 1. Vào Ubuntu cập nhật hệ thống

```bash
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ sudo apt update
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ sudo apt upgrade -y
```

## 2. Cài các Tool nền tảng

```bash
sudo apt install -y \
  curl \
  wget \
  git \
  unzip \
  sqlite3 \
  python3 \
  python3-pip \
  nginx \
  osmium-tool
```

Kiểm tra lại version

```bash
osmium --version
nginx -v
python3 --version
pip3 --version
sqlite3 --version
```

```bash
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ osmium --version
osmium version 1.19.0
libosmium version 2.23.0
Supported PBF compression types: none zlib lz4

Copyright (C) 2013-2026  Jochen Topf <jochen@topf.org>
License: GNU GENERAL PUBLIC LICENSE Version 3 <https://gnu.org/licenses/gpl.html>.
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ nginx -v
nginx version: nginx/1.28.3 (Ubuntu)
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ python3 --version
Python 3.14.4
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ pip3 --version
pip 25.1.1 from /usr/lib/python3/dist-packages/pip (python 3.14)
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ sqlite3 --version
3.46.1 2024-08-13 09:16:08 c9c2ab54ba1f5f46360f1b4f35d849cd3f080e6fc2b6c60e91b16c63f69aalt1 (64-bit)
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
```

## 3. Cấu hình cấu trúc thư mục project:

```bash
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ mkdir -p {data,out,public/tiles,nginx,scripts,screenshots}
```

Kiểm tra cây thư mục:
```bash
pwd

# kết quả
/mnt/d/A-VinVSF/gis-offline-demo
```

Kết quả cây thư mục

```bash
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ find . -maxdepth 2 -type d | sort
.
./.idea
./.idea/inspectionProfiles
./.venv
./.venv/Include
./.venv/Lib
./.venv/Scripts
./data
./nginx
./out
./public
./public/tiles
./screenshots
./scripts
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
```

## 4. Tải dữ liệu vietnam-lastest.osm.pbf

Tải dữ liệu về

```bash
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ cat > scripts/01_download_data.sh <<'EOF'
#!/bin/bash
set -e

mkdir -p data

echo "[INFO] Downloading Vietnam OSM PBF..."
wget -c -O data/vietnam-latest.osm.pbf \
  https://download.geofabrik.de/asia/vietnam-latest.osm.pbf

echo "[INFO] Download completed."
ls -lh data/vietnam-latest.osm.pbf

echo "[INFO] Basic file info:"
file data/vietnam-latest.osm.pbf

echo "[INFO] Osmium file info:"
osmium fileinfo data/vietnam-latest.osm.pbf
EOF
```

- Chạy `sh scripts/01_download_data.sh`

- Sau khi tải xong, chạy thêm: `osmium fileinfo -e data/vietnam-latest.osm.pbf`

- Kết quả: 

```bash
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ osmium fileinfo -e data/vietnam-latest.osm.pbf
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
[======================================================================] 100% 
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
  Sum of buffer capacities: 3952803840 (3.769 GB, 97% full)
Metadata:
  All objects have following metadata attributes: version+timestamp
  Some objects have following metadata attributes: version+timestamp
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
```


Chạy lại các lệnh:
```bash
ls -lh data/
osmium fileinfo data/vietnam-latest.osm.pbf
osmium fileinfo -e data/vietnam-latest.osm.pbf | head -n 40
```


## 5. Chạy code sh để chỉ lấy dữ liệu map của Hà Nội

Chạy `sh scripts/02_extract_hanoi.sh`

Chạy kiểm tra chi tiết 2 file `hanoi.osm.pbf` và `hanoi_roads.osm.pbf`: lệnh `osmium fileinfo -e data/hanoi.osm.pbf | head -n 40` và lệnh `osmium fileinfo -e data/hanoi_roads.osm.pbf | head -n 40`

Kiểm tra dung lượng `ls -lh data/`

Kết quả:

```bash
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ sh scripts/02_extract_hanoi.sh
[INFO] Input file:
-rwxrwxrwx 1 cong2004 cong2004 309M May 24 22:34 data/vietnam-latest.osm.pbf
[INFO] Extracting Ha Noi area...
[======================================================================] 100% 
[INFO] Ha Noi extract created:
-rwxrwxrwx 1 cong2004 cong2004 27M May 25 10:05 data/hanoi.osm.pbf
[INFO] Ha Noi extract fileinfo:
File:
  Name: data/hanoi.osm.pbf
  Format: PBF
  Compression: none
  Size: 27588654
Header:
  Bounding boxes:
  With history: no
  Options:
    generator=osmium/1.19.0
    pbf_dense_nodes=true
    pbf_optional_feature_0=Sort.Type_then_ID
    sorting=Type_then_ID
[INFO] Filtering highway ways from Ha Noi extract...
[======================================================================] 100% 
[INFO] Ha Noi roads file created:
-rwxrwxrwx 1 cong2004 cong2004 16M May 25 10:05 data/hanoi_roads.osm.pbf
[INFO] Ha Noi roads fileinfo:
File:
  Name: data/hanoi_roads.osm.pbf
  Format: PBF
  Compression: none
  Size: 15879306
Header:
  Bounding boxes:
  With history: no
  Options:
    generator=osmium/1.19.0
    pbf_dense_nodes=true
    pbf_optional_feature_0=Sort.Type_then_ID
    sorting=Type_then_ID
[DONE] Step 3 completed.
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ osmium fileinfo -e data/hanoi.osm.pbf | head -n 40
File:
  Name: data/hanoi.osm.pbf
  Format: PBF
  Compression: none
  Size: 27588654
Header:
  Bounding boxes:
  With history: no
  Options:
    generator=osmium/1.19.0
    pbf_dense_nodes=true
    pbf_optional_feature_0=Sort.Type_then_ID
    sorting=Type_then_ID
Data:
  Bounding box: (104.7092046,20.1231883,106.7530801,21.6373892)
  Timestamps:
    First: 2007-10-16T12:57:06Z
    Last: 2026-05-24T19:25:24Z
  Objects ordered (by type and id): yes
  Multiple versions of same object: no
  CRC32: not calculated (use --crc/-c to enable)
  Number of changesets: 0
  Number of nodes: 3227923
  Number of ways: 542604
  Number of relations: 4037
  Smallest changeset ID: 0
  Smallest node ID: 74126839
  Smallest way ID: 9566396
  Smallest relation ID: 13430
  Largest changeset ID: 0
  Largest node ID: 13873834440
  Largest way ID: 1520955359
  Largest relation ID: 20729843
  Number of buffers: 4874 (avg 774 objects per buffer)
  Sum of buffer sizes: 308804968 (0.294 GB)
  Sum of buffer capacities: 321323008 (0.306 GB, 96% full)
Metadata:
  All objects have following metadata attributes: version+timestamp
  Some objects have following metadata attributes: version+timestamp
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ osmium fileinfo -e data/hanoi_roads.osm.pbf | head -n 40
File:
  Name: data/hanoi_roads.osm.pbf
  Format: PBF
  Compression: none
  Size: 15879306
Header:
  Bounding boxes:
  With history: no
  Options:
    generator=osmium/1.19.0
    pbf_dense_nodes=true
    pbf_optional_feature_0=Sort.Type_then_ID
    sorting=Type_then_ID
Data:
  Bounding box: (105.0958807,20.2497056,106.1389386,21.3294731)
  Timestamps:
    First: 2007-10-21T12:30:28Z
    Last: 2026-05-24T19:25:24Z
  Objects ordered (by type and id): yes
  Multiple versions of same object: no
  CRC32: not calculated (use --crc/-c to enable)
  Number of changesets: 0
  Number of nodes: 1712206
  Number of ways: 276191
  Number of relations: 0
  Smallest changeset ID: 0
  Smallest node ID: 75617751
  Smallest way ID: 9566542
  Smallest relation ID: 0
  Largest changeset ID: 0
  Largest node ID: 13873705561
  Largest way ID: 1520518905
  Largest relation ID: 0
  Number of buffers: 2494 (avg 797 objects per buffer)
  Sum of buffer sizes: 158279856 (0.15 GB)
  Sum of buffer capacities: 163446784 (0.155 GB, 97% full)
Metadata:
  All objects have following metadata attributes: version+timestamp
  Some objects have following metadata attributes: version+timestamp
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
cong2004@DESKTOP-33JPL77:/mnt/d/A-VinVSF/gis-offline-demo$ 
```



