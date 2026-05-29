package gis.gis_demo.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TilemakerConfigUtil {

    // Thread-safe and reusable singleton ObjectMapper
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static void generateTilemakerConfig(int batchMinZoom, int batchMaxZoom, TileMath.BBox batchBbox,
            Path originalConfig, Path batchConfig) throws Exception {
        Map<String, Object> configMap = MAPPER.readValue(originalConfig.toFile(), Map.class);
        Map<String, Object> settings = (Map<String, Object>) configMap.get("settings");
        settings.put("minzoom", batchMinZoom);
        settings.put("maxzoom", batchMaxZoom);
        settings.put("basezoom", batchMaxZoom);
        settings.put("bounds", List.of(batchBbox.minLon(), batchBbox.minLat(), batchBbox.maxLon(), batchBbox.maxLat()));
        settings.put("name", "patch_z" + batchMinZoom + "_z" + batchMaxZoom);
        settings.put("description", "Patch vector tiles for zoom " + batchMinZoom + "-" + batchMaxZoom);
        MAPPER.writeValue(batchConfig.toFile(), configMap);
    }
}
