package gis.gis_demo.service;

import gis.gis_demo.util.TileMath;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

@Service
public class PatchService {
    private static final String SOURCE_PBF = "data/vietnam-latest.osm.pbf";
    private static final int MIN_ZOOM = 4;
    private static final int MAX_ZOOM = 16;
    private static final int PATCH_TILESERVER_PORT = 8091;
    private static final int[][] PATCH_ZOOM_BATCHES = {
            {4, 8},
            {9, 12},
            {13, 16}
    };

    private final Path projectRoot = Paths.get("").toAbsolutePath();

    private void log(Path logFile, String message) {
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(
                    logFile,
                    java.time.LocalDateTime.now() + " " + message + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException("Cannot write log file: " + logFile, e);
        }
    }

    private int normalizeMinZoom(int minZoom) {
        return minZoom <= 0 ? MIN_ZOOM : Math.max(MIN_ZOOM, minZoom);
    }

    private int normalizeMaxZoom(int maxZoom) {
        return maxZoom <= 0 ? MAX_ZOOM : Math.min(MAX_ZOOM, maxZoom);
    }

    private String tilemakerConfigForZoomRange(int minZoom, int maxZoom) {
        return """
        {
          "layers": {
            "landuse": { "minzoom": %d, "maxzoom": %d },
            "water": { "minzoom": %d, "maxzoom": %d },
            "buildings": { "minzoom": %d, "maxzoom": %d },
            "roads": { "minzoom": %d, "maxzoom": %d },
            "planned_roads": { "minzoom": %d, "maxzoom": %d },
            "poi": { "minzoom": %d, "maxzoom": %d },
            "water_lines": { "minzoom": %d, "maxzoom": %d }
          },
          "settings": {
            "minzoom": %d,
            "maxzoom": %d,
            "basezoom": %d,
            "include_ids": false,
            "combine_below": %d,
            "name": "patch_z%d_z%d",
            "version": "1.0",
            "description": "Patch vector tiles for zoom %d-%d",
            "compress": "gzip",
            "bounds": [105.30, 20.45, 106.10, 21.40],
            "center": [105.85, 21.03, 11]
          }
        }
        """.formatted(
                minZoom, maxZoom,
                minZoom, maxZoom,
                minZoom, maxZoom,
                minZoom, maxZoom,
                minZoom, maxZoom,
                minZoom, maxZoom,
                minZoom, maxZoom,
                minZoom,
                maxZoom,
                maxZoom,
                minZoom,
                minZoom, maxZoom,
                minZoom, maxZoom
        );
    }

    private List<TileMath.Tile> filterTilesByZoomRange( List<TileMath.Tile> tiles, int minZoom, int maxZoom) {
        return tiles.stream()
                .filter(tile -> tile.z() >= minZoom && tile.z() <= maxZoom)
                .toList();
    }

    public String runPatch(String bbox, String patchName, int minZoom, int maxZoom) throws Exception {

        int zMin = normalizeMinZoom(minZoom);
        int zMax = normalizeMaxZoom(maxZoom);

        Path patchDir = projectRoot.resolve("patches").resolve(patchName);

        if (Files.exists(patchDir)) {
            deleteDirectory(patchDir);
        }

        Files.createDirectories(patchDir);

        Path logFile = patchDir.resolve("patch.log");
//        Path mainMbtiles = projectRoot.resolve("output").resolve("hanoi.mbtiles");
        Path mainMbtiles = projectRoot.resolve("output").resolve("vietnam.mbtiles");

        log(logFile, "===== START EXACT PER-TILE PATCH " + patchName + " =====");
        log(logFile, "Project root: " + projectRoot);
        log(logFile, "User selected bbox: " + bbox);
        log(logFile, "Zoom: " + zMin + "-" + zMax);

        backupMainMbtiles(mainMbtiles, patchDir, logFile);

        List<TileMath.Tile> allAffectedTiles = TileMath.tilesForBbox(bbox, zMin, zMax);

        log(logFile, "Affected tiles total: " + allAffectedTiles.size());

        for (int[] batch : PATCH_ZOOM_BATCHES) {
            int batchMinZoom = Math.max(zMin, batch[0]);
            int batchMaxZoom = Math.min(zMax, batch[1]);

            if (batchMinZoom > batchMaxZoom) {
                continue;
            }

            List<TileMath.Tile> tilesInBatch = filterTilesByZoomRange(
                    allAffectedTiles,
                    batchMinZoom,
                    batchMaxZoom
            );

            if (tilesInBatch.isEmpty()) {
                continue;
            }

            log(logFile, "----- Processing zoom batch "
                    + batchMinZoom + "-" + batchMaxZoom + " -----");
            log(logFile, "Affected tiles in batch: " + tilesInBatch.size());

            TileMath.BBox batchBbox = TileMath.unionBboxForTiles(tilesInBatch);

            log(logFile, "Full tile-cover bbox for batch "
                    + batchMinZoom + "-" + batchMaxZoom + ": "
                    + batchBbox.toOsmiumBbox());

            Path batchDir = patchDir.resolve("z" + batchMinZoom + "_z" + batchMaxZoom);
            Files.createDirectories(batchDir);

            Path batchPbf = batchDir.resolve("z" + batchMinZoom + "_z" + batchMaxZoom + ".osm.pbf");
            Path batchMbtiles = batchDir.resolve("z" + batchMinZoom + "_z" + batchMaxZoom + ".mbtiles");
            Path batchConfig = batchDir.resolve("tilemaker-z" + batchMinZoom + "-z" + batchMaxZoom + ".json");

            Files.writeString(batchConfig, tilemakerConfigForZoomRange(batchMinZoom, batchMaxZoom));

            runCommand(List.of(
                    "osmium", "extract",
                    "--strategy", "complete_ways",
                    "-b", batchBbox.toOsmiumBbox(),
                    SOURCE_PBF,
                    "-o", batchPbf.toString(),
                    "--overwrite"
            ), logFile);

            Files.deleteIfExists(batchMbtiles);

            runCommand(List.of(
                    "docker", "run", "--rm",
                    "-v", projectRoot + ":/data",
                    "ghcr.io/systemed/tilemaker:master",
                    "/data/" + projectRoot.relativize(batchPbf),
                    "--output", "/data/" + projectRoot.relativize(batchMbtiles),
                    "--config", "/data/" + projectRoot.relativize(batchConfig),
                    "--process", "/data/tilemaker/process.lua"
            ), logFile);

            upsertSelectedTilesFromPatchMbtiles(
                    batchMbtiles,
                    mainMbtiles,
                    tilesInBatch,
                    logFile
            );
        }

        restartTileServerContainer(logFile);

        log(logFile, "===== EXACT PER-TILE PATCH DONE =====");

        return patchDir.toString();
    }

    /*
    private void exportVectorTilesFromMbtiles(Path patchMbtiles, Path outputDir, int minZoom, int maxZoom, Path logFile) throws Exception {
        Files.createDirectories(outputDir);
        String url = "jdbc:sqlite:" + patchMbtiles.toAbsolutePath();
        String sql = """
            SELECT zoom_level, tile_column, tile_row, tile_data
            FROM tiles
            WHERE zoom_level BETWEEN ? AND ?
            ORDER BY zoom_level, tile_column, tile_row
            """;
        int count =0;
        try (var conn = java.sql.DriverManager.getConnection(url); var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minZoom);
            ps.setInt(2, maxZoom);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    int z = rs.getInt("zoom_level");
                    int x = rs.getInt("tile_column");
                    int tmsY = rs.getInt("tile_row");
                    int xyzY = ((1 << z) - 1) - tmsY;
                    byte[] data = rs.getBytes("tile_data");
                    Path tilePath = outputDir
                            .resolve(String.valueOf(z))
                            .resolve(String.valueOf(x))
                            .resolve(xyzY + ".pbf");
                    Files.createDirectories(tilePath.getParent());
                    Files.write(tilePath, data);
                    count++;
                }
            }
        }
        log(logFile, "Exported vector tiles: " + count);
    }

     */

    private void runCommand(List<String> command, Path logFile) throws IOException, InterruptedException {
        log(logFile, "$ " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectRoot.toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

        Process process = pb.start();
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command) + ". See log: " + logFile);
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;

        try (var paths = Files.walk(dir)) {
            paths
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete: " + path, e);
                        }
                    });
        }
    }

    /*
    private void mergePatchMbtilesIntoMain( Path patchMbtiles, Path mainMbtiles, int minZoom, int maxZoom, Path logFile ) throws Exception {
        if (!Files.exists(patchMbtiles)) {
            throw new IllegalArgumentException("Patch MBTiles not found: " + patchMbtiles);
        }

        if (!Files.exists(mainMbtiles)) {
            throw new IllegalArgumentException("Main MBTiles not found: " + mainMbtiles);
        }

        log(logFile, "Merging patch MBTiles into main MBTiles...");
        log(logFile, "Patch: " + patchMbtiles);
        log(logFile, "Main: " + mainMbtiles);
        log(logFile, "Zoom filter: " + minZoom + "-" + maxZoom);

        String mainUrl = "jdbc:sqlite:" + mainMbtiles.toAbsolutePath();

        try (var mainConn = java.sql.DriverManager.getConnection(mainUrl); var stmt = mainConn.createStatement()) {
            stmt.execute("ATTACH DATABASE '" + patchMbtiles.toAbsolutePath() + "' AS patchdb");
            mainConn.setAutoCommit(false);
            int deleted = stmt.executeUpdate("""
                DELETE FROM tiles
                WHERE zoom_level BETWEEN %d AND %d
                  AND EXISTS (
                    SELECT 1
                    FROM patchdb.tiles p
                    WHERE p.zoom_level = tiles.zoom_level
                      AND p.tile_column = tiles.tile_column
                      AND p.tile_row = tiles.tile_row
                  )
                """.formatted(minZoom, maxZoom));

            int inserted = stmt.executeUpdate("""
                INSERT INTO tiles (zoom_level, tile_column, tile_row, tile_data)
                SELECT zoom_level, tile_column, tile_row, tile_data
                FROM patchdb.tiles
                WHERE zoom_level BETWEEN %d AND %d
                """.formatted(minZoom, maxZoom));

            mainConn.commit();

            log(logFile, "Deleted old overlapping tiles: " + deleted);
            log(logFile, "Inserted patch tiles: " + inserted);

            stmt.execute("DETACH DATABASE patchdb");
        }
    }

     */


    private void backupMainMbtiles(Path mainMbtiles, Path patchDir, Path logFile) throws IOException {
        Path backupDir = projectRoot.resolve("output").resolve("backups");
        Files.createDirectories(backupDir);

//        String backupName = "hanoi_before_" + patchDir.getFileName() + ".mbtiles";
        String backupName = "vietnam_before_" + patchDir.getFileName() + ".mbtiles";
        Path backupPath = backupDir.resolve(backupName);

        log(logFile, "Backing up main MBTiles...");
        log(logFile, "Backup: " + backupPath);

        Files.copy(mainMbtiles, backupPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void restartTileServerContainer(Path logFile) throws IOException, InterruptedException {
        log(logFile, "Restarting main TileServer container on port 8081...");
        runCommand(List.of(
                "bash", "-lc",
                "docker ps --filter publish=8081 -q | xargs -r docker restart"
        ), logFile);
        log(logFile, "TileServer restart requested.");
    }

    private void upsertSelectedTilesFromPatchMbtiles(Path patchMbtiles, Path mainMbtiles, List<TileMath.Tile> targetTiles, Path logFile) throws Exception {

        if (!Files.exists(patchMbtiles)) {
            throw new IllegalArgumentException("Patch MBTiles not found: " + patchMbtiles);
        }

        if (!Files.exists(mainMbtiles)) {
            throw new IllegalArgumentException("Main MBTiles not found: " + mainMbtiles);
        }

        if (targetTiles == null || targetTiles.isEmpty()) {
            log(logFile, "No target tiles to upsert.");
            return;
        }

        java.util.Set<String> targetKeys = new java.util.HashSet<>();

        for (TileMath.Tile tile : targetTiles) {
            int tmsY = TileMath.xyzYToTmsY(tile.z(), tile.y());
            targetKeys.add(tile.z() + "/" + tile.x() + "/" + tmsY);
        }

        log(logFile, "Upserting selected tiles from: " + patchMbtiles);
        log(logFile, "Target tile count: " + targetKeys.size());

        String patchUrl = "jdbc:sqlite:" + patchMbtiles.toAbsolutePath();
        String mainUrl = "jdbc:sqlite:" + mainMbtiles.toAbsolutePath();

        int scanned = 0;
        int upserted = 0;

        try (var patchConn = java.sql.DriverManager.getConnection(patchUrl);
             var mainConn = java.sql.DriverManager.getConnection(mainUrl)) {

            try (var mainStmt = mainConn.createStatement()) {
                mainStmt.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS tile_index
                    ON tiles (zoom_level, tile_column, tile_row)
                    """);
            }

            mainConn.setAutoCommit(false);

            String selectSql = """
                SELECT zoom_level, tile_column, tile_row, tile_data
                FROM tiles
                """;

            String upsertSql = """
                INSERT INTO tiles (zoom_level, tile_column, tile_row, tile_data)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(zoom_level, tile_column, tile_row)
                DO UPDATE SET tile_data = excluded.tile_data
                """;

            try (var select = patchConn.createStatement();
                 var rs = select.executeQuery(selectSql);
                 var upsert = mainConn.prepareStatement(upsertSql)) {

                while (rs.next()) {
                    scanned++;

                    int z = rs.getInt("zoom_level");
                    int x = rs.getInt("tile_column");
                    int tmsY = rs.getInt("tile_row");

                    String key = z + "/" + x + "/" + tmsY;

                    if (!targetKeys.contains(key)) {
                        continue;
                    }

                    byte[] data = rs.getBytes("tile_data");

                    upsert.setInt(1, z);
                    upsert.setInt(2, x);
                    upsert.setInt(3, tmsY);
                    upsert.setBytes(4, data);
                    upsert.addBatch();

                    upserted++;
                }

                upsert.executeBatch();
            }

            mainConn.commit();
        }

        log(logFile, "Patch tiles scanned: " + scanned);
        log(logFile, "Tiles upserted into main MBTiles: " + upserted);
    }
}