package gis.gis_demo.service;

import gis.gis_demo.util.TileMath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class TileExportService {

    // Singleton HttpClient for better performance and resource management
    private final HttpClient httpClient;

    public TileExportService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void exportRasterTilesAsPng(Path outputDir, List<TileMath.Tile> targetTiles, Path logFile) {
        if (targetTiles == null || targetTiles.isEmpty())
            return;

        LogService.log(logFile, "Starting Raster PNG export from TileServer GL...");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            LogService.log(logFile, "Failed to create PNG output dir: " + e.getMessage());
            return;
        }

        int count = 0;
        for (TileMath.Tile tile : targetTiles) {
            String tileUrl = String.format("http://localhost:8081/styles/vietnam/%d/%d/%d.png", tile.z(), tile.x(), tile.y());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tileUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(45))
                    .build();

            try {
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    Path tilePath = outputDir
                            .resolve(String.valueOf(tile.z()))
                            .resolve(String.valueOf(tile.x()))
                            .resolve(tile.y() + ".png");
                    Files.createDirectories(tilePath.getParent());
                    Files.write(tilePath, response.body());
                    count++;
                } else {
                    LogService.log(logFile, "[WARN] Failed to fetch PNG: " + tileUrl + " (HTTP " + response.statusCode() + ")");
                }
            } catch (Exception e) {
                LogService.log(logFile, "[ERROR] Exception fetching PNG: " + tileUrl + " -> " + e.getMessage());
            }
        }
        LogService.log(logFile, "Exported " + count + " PNG raster tiles successfully.");
    }
}
