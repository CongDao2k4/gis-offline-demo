function enableRegionSelect() {
    regionSelectMode = true;
    dragStart = null;
    map.dragPan.disable();

    let buttonsHtml = '';
    if (isPatching) {
        buttonsHtml = `
            <div class="processing-msg">⏳ Hệ thống đang vá lỗi...</div>
            <button class="btn btn-secondary" onclick="cancelRegionSelect()">Cancel</button>
        `;
    } else {
        buttonsHtml = `<button class="btn btn-secondary" onclick="cancelRegionSelect()">Cancel</button>`;
    }

    document.getElementById("debug-panel").innerHTML = `
    <h3>Region Select Mode</h3>
    <p>Giữ chuột và kéo để chọn vùng lỗi.<br/>Thả chuột để tạo bbox patch.</p>
    <hr/>
    ${buttonsHtml}
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

    let buttonsHtml = '';
    if (isPatching) {
        buttonsHtml = `
            <div class="processing-msg">⏳ Hệ thống đang vá lỗi...</div>
            <div class="btn-group">
                <button class="btn btn-success" onclick="findChanges('${bboxStr}')">Find Changes</button>
                <button class="btn btn-secondary" onclick="enableRegionSelect()">Select Again</button>
            </div>
        `;
    } else {
        buttonsHtml = `
            <div class="btn-group">
                <button class="btn btn-danger" onclick="runPatch()">Run Patch</button>
                <button class="btn btn-success" onclick="findChanges('${bboxStr}')">Find Changes</button>
                <button class="btn btn-secondary" onclick="enableRegionSelect()">Select Again</button>
            </div>
        `;
    }

    document.getElementById("debug-panel").innerHTML = `
    <h3>Selected Region</h3>
    <div>Zoom: ${MIN_ZOOM}-${MAX_ZOOM}</div>
    <hr/>
    <div>Patch BBOX:</div>
    <code>${bboxStr}</code>
    ${buttonsHtml}
  `;
}

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
