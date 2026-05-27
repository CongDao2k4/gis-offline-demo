# Biến đổi file mbtiles đã có ở thư mục (root)/output ra dạng xem được frontend.

## 1. Prepare

Mở `wsl -d Ubuntu` sau đó tạo folder: `mkdir -p src/main/resources/static/map`

Tạo file `src/main/resources/static/map/index.html`

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Hanoi Roads Offline Map</title>

    <link href="http://localhost:8081/maplibre-gl.css" rel="stylesheet" />
    <script src="http://localhost:8081/maplibre-gl.js"></script>

    <style>
        html, body, #map {  margin: 0;  width: 100%;  height: 100%;}
        #map {  background: #f3f3f3;}
    </style>
</head>

<body>
<div id="map"></div>

<script>
    const map = new maplibregl.Map({
      container: "map",
      center: [105.85, 21.03],
      zoom: 14,
      style: {
        version: 8,
        sources: {
          hanoi_roads: {
            type: "vector",
            tiles: [
              "http://localhost:8081/data/hanoi_roads/{z}/{x}/{y}.pbf"
            ],
            minzoom: 6,
            maxzoom: 14
          }
        },
        layers: [
          {
            id: "background",
            type: "background",
            paint: {
              "background-color": "#f3f3f3"
            }
          },
          {
            id: "roads",
            type: "line",
            source: "hanoi_roads",
            "source-layer": "roads",
            paint: {
              "line-color": [
                "match",
                ["get", "class"],

                "motorway", "#ff0000",
                "trunk", "#ff5500",
                "primary", "#ff8800",
                "secondary", "#ffcc00",
                "tertiary", "#ffee88",
                "residential", "#999999",

                "#3388ff"
                ],
              "line-width": [
                "interpolate", ["linear"],
                ["zoom"],
                6, 0.3,
                10, 1,
                12, 2,
                14, 4
              ]
            }
          }
        ]
      }
    });

    map.addControl(new maplibregl.NavigationControl());

    map.on("click", (e) => {
      const lng = e.lngLat.lng;
      const lat = e.lngLat.lat;

      new maplibregl.Popup()
        .setLngLat([lng, lat])
        .setHTML(`
          <b>Selected location</b><br/>
          Lng: ${lng.toFixed(6)}<br/>
          Lat: ${lat.toFixed(6)}
        `)
        .addTo(map);

      console.log("Clicked:", lng, lat);
    });

    map.on("click", "roads", (e) => {
      const feature = e.features[0];

      new maplibregl.Popup()
        .setLngLat(e.lngLat)
        .setHTML(`
          <b>Road</b><br/>
          Name: ${feature.properties.name || "Không có tên"}<br/>
          Class: ${feature.properties.class || "unknown"}
        `)
        .addTo(map);

      console.log(feature.properties);
    });

    map.on("mouseenter", "roads", () => {
      map.getCanvas().style.cursor = "pointer";
    });

    map.on("mouseleave", "roads", () => {
      map.getCanvas().style.cursor = "";
    });
</script>
</body>
</html>
```

## 2. Chạy docker để chạy Tile Server cho MBTiles

Dùng Docker để chỉ mount dữ liệu file .mbtiles trong thư mục output.

```bash
docker run --rm -it \
  -v "$PWD/output":/data \
  -p 8081:8080 \
  maptiler/tileserver-gl \
  --mbtiles /data/hanoi_roads.mbtiles
```

### Muốn chỉnh chu giao diện khi mở `localhost:8081` thì :

- Tạo thư mục: `mkdir -p tileserver/styles`
- Tạo json `tileserver/config.json`: `nano tileserver/config.json` với nội dung
```json

```

- Tạo CSS style cho Hanoi road: `nano tileserver/styles/hanoi-roads-style.json`


- **Chạy lại docker bằng code mới**:
```bash
docker run --rm -it \
  -v "$PWD/output":/data \
  -v "$PWD/tileserver":/config \
  -p 8081:8080 \
  maptiler/tileserver-gl \
  --config /config/config.json
```

Test xem có cấu hình file json  chưa: `curl http://localhost:8081/styles/hanoi-roads/style.json`

Sau đó mở `http://localhost:8081/styles/hanoi-roads/`

## Sau đó chạy Spring Boot lên.

## Nếu chạy được , giao diện html sẽ liên kết đến tile server (8081) sẽ mở ở: `http://localhost:8080/map/index.html` để xem.


