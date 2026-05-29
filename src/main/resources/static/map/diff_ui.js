async function findChanges(bboxStr) {
    const diffPanel = document.getElementById("diff-panel");
    diffPanel.style.display = "block";
    diffPanel.innerHTML = "<b>Đang tải dữ liệu thay đổi...</b><br/><button onclick='closeDiffPanel()'>Đóng</button>";
    
    try {
        const res = await fetch(`/api/compare?bbox=${bboxStr}`);
        if (!res.ok) throw new Error("API failed");
        const data = await res.json();
        renderDiffList(data);
    } catch (err) {
        console.error(err);
        diffPanel.innerHTML = "<b>Lỗi khi tải dữ liệu diff!</b><br/><button onclick='closeDiffPanel()'>Đóng</button>";
    }
}

function renderDiffList(data) {
    const diffPanel = document.getElementById("diff-panel");
    if (data.length === 0) {
        diffPanel.innerHTML = "<b>Không tìm thấy thay đổi nào trong vùng này!</b><br/><button onclick='closeDiffPanel()'>Đóng</button>";
        return;
    }
    
    let html = `<b>Tìm thấy ${data.length} thay đổi</b><br/>
                <button onclick='closeDiffPanel()'>Đóng</button><hr/>
                <ul style="list-style: none; padding: 0;">`;
    
    data.forEach(item => {
        let color = item.changeType === "NEW" ? "green" : (item.changeType === "DELETED" ? "red" : "orange");
        let badge = `<span style="background:${color}; color:white; padding: 2px 4px; border-radius:3px; font-size:10px;">${item.changeType}</span>`;
        let geojsonSafe = item.geojson ? item.geojson.replace(/'/g, "\\'").replace(/"/g, "&quot;") : "";
        html += `
            <li style="margin-bottom: 8px; border-bottom: 1px solid #ccc; padding-bottom: 5px; cursor: pointer;"
                onclick="flyToDiffItem('${geojsonSafe}', '${item.id}')">
                ${badge} <b>${item.type}</b>: ${item.name} <br/>
                <small>ID: ${item.id}</small>
            </li>
        `;
    });
    
    html += "</ul>";
    diffPanel.innerHTML = html;
}

function closeDiffPanel() {
    document.getElementById("diff-panel").style.display = "none";
    if (map.getLayer("diff-highlight-line")) map.removeLayer("diff-highlight-line");
    if (map.getLayer("diff-highlight-fill")) map.removeLayer("diff-highlight-fill");
    if (map.getSource("diff-highlight")) map.removeSource("diff-highlight");
}

function flyToDiffItem(geojsonStr, id) {
    if (!geojsonStr) {
        alert("Vật thể này không có dữ liệu hình học hợp lệ.");
        return;
    }
    
    const geom = JSON.parse(geojsonStr.replace(/&quot;/g, '"'));
    let centerLng, centerLat;
    
    if (geom.type === "Point") {
        centerLng = geom.coordinates[0];
        centerLat = geom.coordinates[1];
    } else if (geom.type === "MultiPoint") {
        centerLng = geom.coordinates[0][0];
        centerLat = geom.coordinates[0][1];
    } else if (geom.type === "LineString") {
        let mid = Math.floor(geom.coordinates.length / 2);
        centerLng = geom.coordinates[mid][0];
        centerLat = geom.coordinates[mid][1];
    } else if (geom.type === "MultiLineString") {
        let mid = Math.floor(geom.coordinates[0].length / 2);
        centerLng = geom.coordinates[0][mid][0];
        centerLat = geom.coordinates[0][mid][1];
    } else if (geom.type === "Polygon") {
        centerLng = geom.coordinates[0][0][0];
        centerLat = geom.coordinates[0][0][1];
    } else if (geom.type === "MultiPolygon") {
        centerLng = geom.coordinates[0][0][0][0];
        centerLat = geom.coordinates[0][0][0][1];
    } else {
        const center = map.getCenter();
        centerLng = center.lng;
        centerLat = center.lat;
    }
    
    try {
        map.flyTo({
            center: [centerLng, centerLat],
            zoom: 17,
            speed: 1.5
        });
    } catch (e) {
        console.warn("flyTo failed, possibly invalid coordinates", e);
    }
    
    const feature = {
        type: "Feature",
        geometry: geom,
        properties: { id: id }
    };
    
    if (map.getLayer("diff-highlight-line")) map.removeLayer("diff-highlight-line");
    if (map.getLayer("diff-highlight-fill")) map.removeLayer("diff-highlight-fill");
    if (map.getSource("diff-highlight")) map.removeSource("diff-highlight");
    
    map.addSource("diff-highlight", { type: "geojson", data: feature });
    map.addLayer({
        id: "diff-highlight-fill",
        type: "fill",
        source: "diff-highlight",
        paint: { "fill-color": "#ff00ff", "fill-opacity": 0.3 }
    });
    map.addLayer({
        id: "diff-highlight-line",
        type: "line",
        source: "diff-highlight",
        paint: { "line-color": "#ff00ff", "line-width": 4 }
    });
}
