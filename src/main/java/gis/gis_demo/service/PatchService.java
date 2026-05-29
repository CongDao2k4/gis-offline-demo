package gis.gis_demo.service;

import gis.gis_demo.repository.MbtilesRepository;
import gis.gis_demo.util.TileMath;
import gis.gis_demo.util.TilemakerConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.util.List;

@Slf4j
@Service
public class PatchService {
    private static final String SOURCE_PBF = "data/vietnam-latest.osm.pbf";
    private static final String BASE_PBF = "data/vietnam-180101.osm.pbf";
    private static final int MAX_ZOOM = 16;

    private final Path projectRoot = Paths.get("").toAbsolutePath();

    private final MbtilesRepository mbtilesRepository;
    private final TileExportService tileExportService;
    private final CommandRunnerService commandRunnerService;

    // Dependency injection via constructor
    public PatchService(MbtilesRepository mbtilesRepository,
            TileExportService tileExportService,
            CommandRunnerService commandRunnerService) {
        this.mbtilesRepository = mbtilesRepository;
        this.tileExportService = tileExportService;
        this.commandRunnerService = commandRunnerService;
    }

    private int normalizeMaxZoom(int maxZoom) {
        return maxZoom <= 0 ? MAX_ZOOM : Math.min(MAX_ZOOM, maxZoom);
    }

    private List<TileMath.Tile> filterTilesByZoomRange(List<TileMath.Tile> tiles, int minZoom, int maxZoom) {
        return tiles.stream()
                .filter(tile -> tile.z() >= minZoom && tile.z() <= maxZoom)
                .toList();
    }

    public String runPatch(String bboxStr, String patchName, int minZoom, int maxZoom) throws Exception {

        TileMath.BBox parsedBbox = TileMath.BBox.parse(bboxStr);
        int zMin = Math.max(4, minZoom);
        int zMax = normalizeMaxZoom(maxZoom);

        Path patchDir = projectRoot.resolve("patches").resolve(patchName);

        if (Files.exists(patchDir)) {
            FileFolderService.deleteDirectory(patchDir);
        }
        Files.createDirectories(patchDir);

        Path logFile = patchDir.resolve("patch.log");
        Path mainMbtiles = projectRoot.resolve("output").resolve("vietnam.mbtiles");

        LogService.log(logFile, "===== START EXACT PER-TILE BATCH - " + patchName + " =====");
        LogService.log(logFile, "Project root: " + projectRoot);
        LogService.log(logFile, "User selected bbox: " + bboxStr);
        LogService.log(logFile, "Zoom: " + zMin + "-" + zMax);

        Path localPatchOsc = deriveLocalPatchOsc(patchDir, parsedBbox, logFile);

        FileFolderService.backupMainMbtiles(mainMbtiles, patchDir, logFile);

        List<TileMath.Tile> allAffectedTiles = TileMath.tilesForBbox(bboxStr, zMin, zMax);
        LogService.log(logFile, "Affected tiles total: " + allAffectedTiles.size());

        List<int[]> batches = generateZoomBatches(zMin);

        for (int[] batch : batches) {
            int batchMinZoom = batch[0];
            int batchMaxZoom = batch[1];

            if (batchMinZoom > batchMaxZoom)
                continue;

            List<TileMath.Tile> tilesInBatch = filterTilesByZoomRange(allAffectedTiles, batchMinZoom, batchMaxZoom);
            if (tilesInBatch.isEmpty())
                continue;

            processZoomBatch(batchMinZoom, batchMaxZoom, tilesInBatch, patchDir, localPatchOsc, mainMbtiles, logFile);
        }

        commandRunnerService.restartTileServerContainer(logFile);

        LogService.log(logFile, "Waiting 10s for TileServer to boot before exporting PNGs...");
        Thread.sleep(10000);

        // tileExportService.exportRasterTilesAsPng(patchDir.resolve("export_png"), allAffectedTiles, logFile);

        LogService.log(logFile, "===== EXACT PER-TILE PATCH DONE =====");

        return patchDir.toString();
    }

    private Path deriveLocalPatchOsc(Path patchDir, TileMath.BBox parsedBbox, Path logFile) throws Exception {
        Path oldPatchPbf = patchDir.resolve("old_patch.osm.pbf");
        Path newPatchPbf = patchDir.resolve("new_patch.osm.pbf");
        Path localPatchOsc = patchDir.resolve("local_patch.osc");

        LogService.log(logFile, "Extracting old patch area with complete_ways...");
        commandRunnerService.runCommand(List.of(
                "osmium", "extract",
                "--strategy", "complete_ways",
                "-b", parsedBbox.toOsmiumBbox(),
                BASE_PBF,
                "-o", oldPatchPbf.toString(),
                "--overwrite"), logFile);

        LogService.log(logFile, "Extracting new patch area with complete_ways...");
        commandRunnerService.runCommand(List.of(
                "osmium", "extract",
                "--strategy", "complete_ways",
                "-b", parsedBbox.toOsmiumBbox(),
                SOURCE_PBF,
                "-o", newPatchPbf.toString(),
                "--overwrite"), logFile);

        LogService.log(logFile, "Deriving exact local changes for safety...");
        commandRunnerService.runCommand(List.of(
                "osmium", "derive-changes",
                oldPatchPbf.toString(),
                newPatchPbf.toString(),
                "-o", localPatchOsc.toString()), logFile);

        return localPatchOsc;
    }

    private List<int[]> generateZoomBatches(int zMin) {
        List<int[]> batches = new java.util.ArrayList<>();
        if (zMin <= 8) {
            batches.add(new int[] { zMin, 8 });
            batches.add(new int[] { 9, 12 });
            batches.add(new int[] { 13, 16 });
        } else if (zMin <= 12) {
            batches.add(new int[] { zMin, 12 });
            batches.add(new int[] { 13, 16 });
        } else {
            batches.add(new int[] { zMin, 16 });
        }
        return batches;
    }

    private void processZoomBatch(int batchMinZoom, int batchMaxZoom, List<TileMath.Tile> tilesInBatch,
            Path patchDir, Path localPatchOsc, Path mainMbtiles, Path logFile) throws Exception {

        LogService.log(logFile, "----- SOLVING ZOOM BATCH " + batchMinZoom + "-" + batchMaxZoom + " -----");
        LogService.log(logFile, "TILE IS FIXED IN BATCH: " + tilesInBatch.size());

        TileMath.BBox batchBbox = TileMath.unionBboxForTiles(tilesInBatch);
        LogService.log(logFile, "Full tile-cover bbox for batch " + batchMinZoom + "-" + batchMaxZoom + ": "
                + batchBbox.toOsmiumBbox());

        Path batchDir = patchDir.resolve("z" + batchMinZoom + "_z" + batchMaxZoom);
        Files.createDirectories(batchDir);

        Path baseTilePbf = batchDir.resolve("base_tile.osm.pbf");
        Path mergedTilePbf = batchDir.resolve("merged_tile.osm.pbf");
        Path batchMbtiles = batchDir.resolve("z" + batchMinZoom + "_z" + batchMaxZoom + ".mbtiles");
        Path batchConfig = batchDir.resolve("tilemaker-z" + batchMinZoom + "-z" + batchMaxZoom + ".json");

        Path originalConfig = projectRoot.resolve("tilemaker").resolve("config.json");
        TilemakerConfigUtil.generateTilemakerConfig(batchMinZoom, batchMaxZoom, batchBbox, originalConfig, batchConfig);

        LogService.log(logFile, "Extracting 2km x 2km base tile from 2018 map...");
        commandRunnerService.runCommand(List.of(
                "osmium", "extract",
                "--strategy", "complete_ways",
                "-b", batchBbox.toOsmiumBbox(),
                BASE_PBF,
                "-o", baseTilePbf.toString(),
                "--overwrite"), logFile);

        LogService.log(logFile, "Merging precise changes into base tile...");
        commandRunnerService.runCommand(List.of(
                "osmium", "apply-changes",
                baseTilePbf.toString(),
                localPatchOsc.toString(),
                "-o", mergedTilePbf.toString(),
                "--overwrite"), logFile);

        Files.deleteIfExists(batchMbtiles);

        LogService.log(logFile, "Generating tile with tilemaker...");
        commandRunnerService.runCommand(List.of(
                "docker", "run", "--rm",
                "-v", projectRoot + ":/data",
                "ghcr.io/systemed/tilemaker:master",
                "/data/" + projectRoot.relativize(mergedTilePbf),
                "--output", "/data/" + projectRoot.relativize(batchMbtiles),
                "--config", "/data/" + projectRoot.relativize(batchConfig),
                "--process", "/data/tilemaker/process.lua"), logFile);

        mbtilesRepository.upsertSelectedTilesFromPatchMbtiles(batchMbtiles, mainMbtiles, tilesInBatch, logFile);
        mbtilesRepository.exportVectorTilesFromMbtiles(batchMbtiles, patchDir.resolve("export"), tilesInBatch, logFile);
    }
}