let selectedPatch = null;
let regionSelectMode = false;
let dragStart = null;
let tileCacheBust = Date.now();
let isReloadingAfterPatch = false;
let lastPatchBboxStr = null;

const STYLE_URL = "http://localhost:8081/styles/vietnam/style.json";

const MIN_ZOOM = 4;
const MAX_ZOOM = 16;
const DEFAULT_PATCH_DELTA = 0.01;

const map = new maplibregl.Map({
    container: "map",
    center: [105.85, 21.03],
    zoom: 12,
    minZoom: MIN_ZOOM,
    maxZoom: MAX_ZOOM,
    style: `${STYLE_URL}?v=${tileCacheBust}`,
    transformRequest: (url) => {
        if (url.includes("localhost:8081/data/vietnam/")) {
            const sep = url.includes("?") ? "&" : "?";
            return {
                url: `${url}${sep}v=${tileCacheBust}`
            };
        }
        return { url };
    }
});

map.addControl(new maplibregl.NavigationControl());

function lngLatToTile(lng, lat, z) {
    const clampedLat = Math.max(Math.min(lat, 85.05112878), -85.05112878);
    const latRad = clampedLat * Math.PI / 180;
    const n = Math.pow(2, z);

    let x = Math.floor((lng + 180) / 360 * n);
    let y = Math.floor(
        (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * n
    );

    x = Math.max(0, Math.min(n - 1, x));
    y = Math.max(0, Math.min(n - 1, y));

    return { z, x, y };
}

function bboxAround(lng, lat, delta = DEFAULT_PATCH_DELTA) {
    return {
        minLon: lng - delta,
        minLat: lat - delta,
        maxLon: lng + delta,
        maxLat: lat + delta
    };
}

function bboxToString(bbox) {
    return [
        bbox.minLon.toFixed(6),
        bbox.minLat.toFixed(6),
        bbox.maxLon.toFixed(6),
        bbox.maxLat.toFixed(6)
    ].join(",");
}

function makePatchName(prefix, tile) {
    return `${prefix}_${tile.z}_${tile.x}_${tile.y}_${Date.now()}`;
}

function setSelectedPatch(bboxStr, patchName) {
    selectedPatch = {
        bbox: bboxStr,
        patchName: patchName,
        minZoom: MIN_ZOOM,
        maxZoom: MAX_ZOOM
    };
}

function patchCommandText(bboxStr, patchName) {
    return `POST /api/patch { bbox: "${bboxStr}", patchName: "${patchName}", minZoom: ${MIN_ZOOM}, maxZoom: ${MAX_ZOOM} }`;
}

function updateDebugPanelForCenter() {
    const center = map.getCenter();
    const zoom = Math.floor(map.getZoom());
    const tile = lngLatToTile(center.lng, center.lat, zoom);

    document.getElementById("debug-panel").innerHTML = `
    <b>Map Debug</b><br/>
    Center: ${center.lng.toFixed(6)}, ${center.lat.toFixed(6)}<br/>
    Zoom: ${zoom}<br/>
    Center Tile: ${tile.z}/${tile.x}/${tile.y}<br/>
    Patch Zoom Range: ${MIN_ZOOM}-${MAX_ZOOM}<br/>
    <hr/>
    <button onclick="enableRegionSelect()">Select Region</button>
    <button onclick="useCenterPointPatch()">Patch Center Point</button><br/>
    Kéo chuột sau khi bấm <b>Select Region</b> để chọn bbox lỗi.
  `;
}

function updateDebugPanelForPoint(lng, lat) {
    const zoom = Math.floor(map.getZoom());
    const tile = lngLatToTile(lng, lat, zoom);
    const bboxStr = bboxToString(bboxAround(lng, lat));
    const patchName = makePatchName("point_patch", tile);

    setSelectedPatch(bboxStr, patchName);

    document.getElementById("debug-panel").innerHTML = `
    <b>Selected Error Point</b><br/>
    LngLat: ${lng.toFixed(6)}, ${lat.toFixed(6)}<br/>
    Zoom: ${zoom}<br/>
    Current Tile: ${tile.z}/${tile.x}/${tile.y}<br/>
    Patch Zoom Range: ${MIN_ZOOM}-${MAX_ZOOM}<br/>
    Patch BBOX:<code>${bboxStr}</code>
    API:<code>${patchCommandText(bboxStr, patchName)}</code>
    <button onclick="runPatch()">Run Patch</button>
    <button onclick="enableRegionSelect()">Select Region Instead</button>
  `;

    console.log("Selected point patch:", selectedPatch);
}

function useCenterPointPatch() {
    const center = map.getCenter();
    updateDebugPanelForPoint(center.lng, center.lat);
}

function enableRegionSelect() {
    regionSelectMode = true;
    dragStart = null;
    map.dragPan.disable();

    document.getElementById("debug-panel").innerHTML = `
    <b>Region Select Mode</b><br/>
    Giữ chuột và kéo để chọn vùng lỗi.<br/>
    Thả chuột để tạo bbox patch.<br/>
    <button onclick="cancelRegionSelect()">Cancel</button>
  `;
}

function cancelRegionSelect() {
    regionSelectMode = false;
    dragStart = null;
    map.dragPan.enable();
    document.getElementById("selection-box").style.display = "none";
    updateDebugPanelForCenter();
}

function updateSelectionBox(startPoint, endPoint) {
    const box = document.getElementById("selection-box");

    const minX = Math.min(startPoint.x, endPoint.x);
    const maxX = Math.max(startPoint.x, endPoint.x);
    const minY = Math.min(startPoint.y, endPoint.y);
    const maxY = Math.max(startPoint.y, endPoint.y);

    box.style.display = "block";
    box.style.left = `${minX}px`;
    box.style.top = `${minY}px`;
    box.style.width = `${maxX - minX}px`;
    box.style.height = `${maxY - minY}px`;
}

function screenBoxToLngLatBbox(startPoint, endPoint) {
    const minX = Math.min(startPoint.x, endPoint.x);
    const maxX = Math.max(startPoint.x, endPoint.x);
    const minY = Math.min(startPoint.y, endPoint.y);
    const maxY = Math.max(startPoint.y, endPoint.y);

    const sw = map.unproject([minX, maxY]);
    const ne = map.unproject([maxX, minY]);

    return {
        minLon: sw.lng,
        minLat: sw.lat,
        maxLon: ne.lng,
        maxLat: ne.lat
    };
}

function finishRegionSelect(endPoint) {
    const bbox = screenBoxToLngLatBbox(dragStart, endPoint);
    const bboxStr = bboxToString(bbox);
    const center = map.getCenter();
    const zoom = Math.floor(map.getZoom());
    const tile = lngLatToTile(center.lng, center.lat, zoom);
    const patchName = makePatchName("region_patch", tile);

    setSelectedPatch(bboxStr, patchName);

    document.getElementById("selection-box").style.display = "none";
    regionSelectMode = false;
    dragStart = null;
    map.dragPan.enable();

    document.getElementById("debug-panel").innerHTML = `
    <b>Selected Region</b><br/>
    Patch Zoom Range: ${MIN_ZOOM}-${MAX_ZOOM}<br/>
    Patch BBOX:<code>${bboxStr}</code>
    Patch Name:<code>${patchName}</code>
    API:<code>${patchCommandText(bboxStr, patchName)}</code>
    <button onclick="runPatch()">Run Patch</button>
    <button onclick="enableRegionSelect()">Select Again</button>
  `;

    console.log("Selected region patch:", selectedPatch);
}

function showRoadPopup(e) {
    const feature = e.features[0];
    const p = feature.properties;

    new maplibregl.Popup()
        .setLngLat(e.lngLat)
        .setHTML(`
      <b>Road</b><br/>
      Name: ${p.name || "Không có tên"}<br/>
      Class: ${p.class || "unknown"}<br/>
      Surface: ${p.surface || "unknown"}<br/>
      Lanes: ${p.lanes || "unknown"}<br/>
      Bridge: ${p.bridge || "no"}
    `)
        .addTo(map);

    console.log("Road properties:", p);
}

function showGenericPopup(layerName, title) {
    return function(e) {
        const feature = e.features[0];
        const p = feature.properties;

        new maplibregl.Popup()
            .setLngLat(e.lngLat)
            .setHTML(`
        <b>${title}</b><br/>
        Name: ${p.name || "Không có tên"}<br/>
        Class: ${p.class || p.amenity || p.building || p.leisure || "unknown"}
      `)
            .addTo(map);

        console.log(layerName, p);
    };
}

map.on("load", () => {
    updateDebugPanelForCenter();

    map.on("click", "roads-minor", showRoadPopup);
    map.on("click", "roads-major", showRoadPopup);
    map.on("click", "planned-roads", showGenericPopup("planned-roads", "Planned / Construction Road"));
    map.on("click", "water", showGenericPopup("water", "Water"));
    map.on("click", "water-lines", showGenericPopup("water-lines", "Waterway"));
    map.on("click", "landuse", showGenericPopup("landuse", "Landuse"));
    map.on("click", "buildings", showGenericPopup("buildings", "Building"));
    map.on("click", "poi-labels", showGenericPopup("poi-labels", "POI"));

    [
        "roads-minor",
        "roads-major",
        "planned-roads",
        "water",
        "water-lines",
        "landuse",
        "buildings",
        "poi-labels"
    ].forEach((layerId) => {
        map.on("mouseenter", layerId, () => {
            if (!regionSelectMode) map.getCanvas().style.cursor = "pointer";
        });

        map.on("mouseleave", layerId, () => {
            if (!regionSelectMode) map.getCanvas().style.cursor = "";
        });
    });
});

map.on("moveend", () => {
    if (!regionSelectMode && !isReloadingAfterPatch) {
        updateDebugPanelForCenter();
    }
});

map.on("zoomend", () => {
    if (!regionSelectMode && !isReloadingAfterPatch) {
        updateDebugPanelForCenter();
    }
});

map.on("mousedown", (e) => {
    if (!regionSelectMode) return;
    e.preventDefault();
    dragStart = e.point;
});

map.on("mousemove", (e) => {
    if (!regionSelectMode || !dragStart) return;
    updateSelectionBox(dragStart, e.point);
});

map.on("mouseup", (e) => {
    if (!regionSelectMode || !dragStart) return;
    finishRegionSelect(e.point);
});

map.on("click", (e) => {
    if (regionSelectMode) return;
    updateDebugPanelForPoint(e.lngLat.lng, e.lngLat.lat);
});

async function runPatch() {
    if (!selectedPatch) {
        alert("Chưa chọn vùng/điểm lỗi");
        return;
    }
    try {
        document.getElementById("debug-panel").innerHTML += `
            <hr/>
            <b>Running patch...</b><br/>
            Please wait...
        `;
        const res = await fetch("/api/patch", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(selectedPatch)
        });
        if (!res.ok) {
            const text = await res.text();
            throw new Error(text);
        }
        const data = await res.json();
        await reloadMapAfterPatch(selectedPatch.bbox);
        alert("Patch done and map reloaded: " + data.outputDir);
    } catch (err) {
        console.error(err);
        alert("Patch failed. Xem log backend.");
    }
}

function parseBbox(bboxStr) {
    const [minLon, minLat, maxLon, maxLat] = bboxStr.split(",").map(Number);
    return { minLon, minLat, maxLon, maxLat };
}

function bboxCenter(bbox) {
    return [
        (bbox.minLon + bbox.maxLon) / 2,
        (bbox.minLat + bbox.maxLat) / 2
    ];
}

function bboxPolygon(bbox) {
    return {
        type: "Feature",
        geometry: {
            type: "Polygon",
            coordinates: [[
                [bbox.minLon, bbox.minLat],
                [bbox.maxLon, bbox.minLat],
                [bbox.maxLon, bbox.maxLat],
                [bbox.minLon, bbox.maxLat],
                [bbox.minLon, bbox.minLat]
            ]]
        },
        properties: {}
    };
}

function addPatchHighlight(bboxStr) {
    if (!map.isStyleLoaded()) {
        map.once("style.load", () => addPatchHighlight(bboxStr));
        return;
    }

    const bbox = parseBbox(bboxStr);
    const feature = bboxPolygon(bbox);

    if (map.getLayer("patch-highlight-line")) {
        map.removeLayer("patch-highlight-line");
    }

    if (map.getLayer("patch-highlight-fill")) {
        map.removeLayer("patch-highlight-fill");
    }

    if (map.getSource("patch-highlight")) {
        map.removeSource("patch-highlight");
    }

    map.addSource("patch-highlight", {
        type: "geojson",
        data: feature
    });

    map.addLayer({
        id: "patch-highlight-fill",
        type: "fill",
        source: "patch-highlight",
        paint: {
            "fill-color": "#ff0000",
            "fill-opacity": 0.12
        }
    });

    map.addLayer({
        id: "patch-highlight-line",
        type: "line",
        source: "patch-highlight",
        paint: {
            "line-color": "#ff0000",
            "line-width": 3,
            "line-dasharray": [2, 2]
        }
    });
}

async function waitForTileServerReady(maxAttempts = 30) {
    for (let i = 0; i < maxAttempts; i++) {
        try {
            const res = await fetch(`${STYLE_URL}?check=${Date.now()}`);
            if (res.ok) return;
        } catch (e) {
            console.log("Waiting for TileServer...");
        }

        await new Promise(resolve => setTimeout(resolve, 1000));
    }

    throw new Error("TileServer 8081 chưa sẵn sàng sau khi restart");
}

async function reloadMapAfterPatch(bboxStr) {
    await waitForTileServerReady();

    isReloadingAfterPatch = true;
    lastPatchBboxStr = bboxStr;
    tileCacheBust = Date.now();

    map.setStyle(`${STYLE_URL}?v=${tileCacheBust}`);

    map.once("style.load", () => {
        const bbox = parseBbox(bboxStr);
        const center = bboxCenter(bbox);

        map.flyTo({
            center: center,
            zoom: Math.max(map.getZoom(), 14),
            speed: 1.2
        });

        map.once("idle", () => {
            addPatchHighlight(bboxStr);

            document.getElementById("debug-panel").innerHTML = `
                <b>Patch Applied</b><br/>
                Map reloaded from updated MBTiles.<br/>
                Highlighted BBOX:
                <code>${bboxStr}</code>
                <button onclick="enableRegionSelect()">Select Another Region</button>
            `;

            isReloadingAfterPatch = false;
        });
    });
}