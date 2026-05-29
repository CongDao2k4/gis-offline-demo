function updateDebugPanelForCenter() {
    const center = map.getCenter();
    const zoom = Math.floor(map.getZoom());
    const tile = lngLatToTile(center.lng, center.lat, zoom);

    let buttonsHtml = '';
    if (isPatching) {
        buttonsHtml = `
            <div class="processing-msg">⏳ Hệ thống đang vá lỗi...</div>
            <div class="btn-group">
                <button class="btn btn-primary" onclick="enableRegionSelect()">Select Region</button>
            </div>
            <p style="margin-top: 10px; color: #666;">Bạn vẫn có thể khoanh vùng để Tìm thay đổi (Patch đang bị khóa).</p>
        `;
    } else {
        buttonsHtml = `
            <div class="btn-group">
                <button class="btn btn-primary" onclick="enableRegionSelect()">Select Region</button>
                <button class="btn btn-secondary" onclick="useCenterPointPatch()">Patch Center Point</button>
            </div>
            <p style="margin-top: 10px; color: #666;">Kéo chuột sau khi bấm <b>Select Region</b> để chọn bbox lỗi.</p>
        `;
    }

    document.getElementById("debug-panel").innerHTML = `
    <h3>Map Inspector</h3>
    <div>Center: ${center.lng.toFixed(6)}, ${center.lat.toFixed(6)}</div>
    <div>Zoom: ${zoom} | Tile: ${tile.z}/${tile.x}/${tile.y}</div>
    <hr/>
    ${buttonsHtml}
  `;
}

function updateDebugPanelForPoint(lng, lat) {
    const zoom = Math.floor(map.getZoom());
    const tile = lngLatToTile(lng, lat, zoom);
    const bboxStr = bboxToString(bboxAround(lng, lat));
    const patchName = makePatchName("point_patch", tile);

    setSelectedPatch(bboxStr, patchName);

    let buttonsHtml = '';
    if (isPatching) {
        buttonsHtml = `
            <div class="processing-msg">⏳ Hệ thống đang vá lỗi...</div>
            <div class="btn-group">
                <button class="btn btn-secondary" onclick="enableRegionSelect()">Select Region Instead</button>
            </div>
        `;
    } else {
        buttonsHtml = `
            <div class="btn-group">
                <button class="btn btn-danger" onclick="runPatch()">Run Patch</button>
                <button class="btn btn-secondary" onclick="enableRegionSelect()">Select Region Instead</button>
            </div>
        `;
    }

    document.getElementById("debug-panel").innerHTML = `
    <h3>Selected Error Point</h3>
    <div>LngLat: ${lng.toFixed(6)}, ${lat.toFixed(6)}</div>
    <div>Zoom: ${zoom} | Tile: ${tile.z}/${tile.x}/${tile.y}</div>
    <hr/>
    <div>Patch BBOX:</div>
    <code>${bboxStr}</code>
    ${buttonsHtml}
  `;
}

function useCenterPointPatch() {
    const center = map.getCenter();
    updateDebugPanelForPoint(center.lng, center.lat);
}
