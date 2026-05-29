async function runPatch() {
    if (!selectedPatch) {
        alert("Chưa chọn vùng/điểm lỗi");
        return;
    }
    
    isPatching = true;
    const panel = document.getElementById("debug-panel");
    const btnGroup = panel.querySelector('.btn-group');
    if (btnGroup) {
        btnGroup.outerHTML = `<div class="processing-msg">⏳ Hệ thống đang xử lý Patch...<br/>Vui lòng chờ trong giây lát!</div>`;
    }

    try {
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
        isPatching = false;
        // Optionally revert to original state if failed
        updateDebugPanelForCenter();
    }
}

function addPatchHighlight(bboxStr) {
    if (!map.isStyleLoaded()) {
        map.once("style.load", () => addPatchHighlight(bboxStr));
        return;
    }

    const bbox = parseBbox(bboxStr);
    const feature = bboxPolygon(bbox);

    if (map.getLayer("patch-highlight-line")) map.removeLayer("patch-highlight-line");
    if (map.getLayer("patch-highlight-fill")) map.removeLayer("patch-highlight-fill");
    if (map.getSource("patch-highlight")) map.removeSource("patch-highlight");

    map.addSource("patch-highlight", { type: "geojson", data: feature });
    map.addLayer({
        id: "patch-highlight-fill",
        type: "fill",
        source: "patch-highlight",
        paint: { "fill-color": "#ff0000", "fill-opacity": 0.12 }
    });
    map.addLayer({
        id: "patch-highlight-line",
        type: "line",
        source: "patch-highlight",
        paint: { "line-color": "#ff0000", "line-width": 3, "line-dasharray": [2, 2] }
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
            isPatching = false; // Reset patching state
            addPatchHighlight(bboxStr);
            document.getElementById("debug-panel").innerHTML = `
                <h3>Patch Applied Successfully</h3>
                <p>Map reloaded from updated MBTiles.</p>
                <div>Highlighted BBOX:</div>
                <code>${bboxStr}</code>
                <div class="btn-group">
                    <button class="btn btn-primary" onclick="enableRegionSelect()">Select Another Region</button>
                </div>
            `;
            isReloadingAfterPatch = false;
        });
    });
}
