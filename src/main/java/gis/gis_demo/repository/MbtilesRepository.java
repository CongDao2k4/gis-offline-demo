package gis.gis_demo.repository;

import gis.gis_demo.util.TileMath;
import gis.gis_demo.service.LogService;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Repository
public class MbtilesRepository {

    public void upsertSelectedTilesFromPatchMbtiles(Path patchMbtiles, Path mainMbtiles, List<TileMath.Tile> targetTiles, Path logFile) throws Exception {
        if (!Files.exists(patchMbtiles)) throw new IllegalArgumentException("Patch MBTiles not found: " + patchMbtiles);
        if (!Files.exists(mainMbtiles)) throw new IllegalArgumentException("Main MBTiles not found: " + mainMbtiles);
        if (targetTiles == null || targetTiles.isEmpty()) {
            LogService.log(logFile, "No target tiles to upsert.");
            return;
        }

        java.util.Set<String> targetKeys = new java.util.HashSet<>();
        for (TileMath.Tile tile : targetTiles) {
            int tmsY = TileMath.xyzYToTmsY(tile.z(), tile.y());
            targetKeys.add(tile.z() + "/" + tile.x() + "/" + tmsY);
        }

        LogService.log(logFile, "Upserting selected tiles from: " + patchMbtiles);
        LogService.log(logFile, "Target tile count: " + targetKeys.size());

        String patchUrl = "jdbc:sqlite:" + patchMbtiles.toAbsolutePath();
        String mainUrl = "jdbc:sqlite:" + mainMbtiles.toAbsolutePath();

        int scanned = 0;
        int upserted = 0;

        try (var patchConn = java.sql.DriverManager.getConnection(patchUrl); var mainConn = java.sql.DriverManager.getConnection(mainUrl)) {
            try (var mainStmt = mainConn.createStatement()) {
                mainStmt.execute("""
                        CREATE UNIQUE INDEX IF NOT EXISTS tile_index
                        ON tiles (zoom_level, tile_column, tile_row)
                        """);
            }
            mainConn.setAutoCommit(false);
            String selectSql = "SELECT zoom_level, tile_column, tile_row, tile_data FROM tiles";
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
        LogService.log(logFile, "Patch tiles scanned: " + scanned);
        LogService.log(logFile, "Tiles upserted into main MBTiles: " + upserted);
    }

    public void exportVectorTilesFromMbtiles(Path patchMbtiles, Path outputDir, List<TileMath.Tile> targetTiles, Path logFile) throws Exception {
        if (targetTiles == null || targetTiles.isEmpty()) {
            return;
        }

        Files.createDirectories(outputDir);
        String url = "jdbc:sqlite:" + patchMbtiles.toAbsolutePath();

        java.util.Set<String> targetKeys = new java.util.HashSet<>();
        for (TileMath.Tile tile : targetTiles) {
            int tmsY = TileMath.xyzYToTmsY(tile.z(), tile.y());
            targetKeys.add(tile.z() + "/" + tile.x() + "/" + tmsY);
        }

        String sql = """
                SELECT zoom_level, tile_column, tile_row, tile_data
                FROM tiles
                ORDER BY zoom_level, tile_column, tile_row
                """;
        int count = 0;
        try (var conn = java.sql.DriverManager.getConnection(url); var stmt = conn.createStatement(); var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int z = rs.getInt("zoom_level");
                int x = rs.getInt("tile_column");
                int tmsY = rs.getInt("tile_row");

                String key = z + "/" + x + "/" + tmsY;
                if (!targetKeys.contains(key)) continue;

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
        LogService.log(logFile, "Exported vector tiles to output directory: " + count);
    }
}
