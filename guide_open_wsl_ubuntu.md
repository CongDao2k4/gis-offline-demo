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
Header:
  Bounding boxes
  With history
  Options
Data:
  Number of nodes
  Number of ways
  Number of relations
```




