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

map.on("click", (e) => {
    if (regionSelectMode) return;
    updateDebugPanelForPoint(e.lngLat.lng, e.lngLat.lat);
});
