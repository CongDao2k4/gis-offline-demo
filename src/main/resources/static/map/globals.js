let selectedPatch = null;
let regionSelectMode = false;
let dragStart = null;
let tileCacheBust = Date.now();
let isReloadingAfterPatch = false;
let lastPatchBboxStr = null;
let isPatching = false;

const STYLE_URL = "http://localhost:8081/styles/vietnam/style.json";
const MIN_ZOOM = 4;
const MAX_ZOOM = 16;

// Example dynamic config initialization from backend API
/*
let STYLE_URL = "";
let MIN_ZOOM = 4;
let MAX_ZOOM = 16;

async function loadConfig() {
    const res = await fetch('/api/config');
    const config = await res.json();
    STYLE_URL = `http://${config.tileserverHost}:${config.tileserverPort}/styles/${config.styleName}/style.json`;
    MIN_ZOOM = config.minZoom;
    MAX_ZOOM = config.maxZoom;
}
// Note: If using dynamic config, call loadConfig() and wait for it before initializing the map.
*/
const DEFAULT_PATCH_DELTA = 0.01;

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
