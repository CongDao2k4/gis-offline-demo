//package gis.gis_demo.service;
//
//import gis.gis_demo.util.TileMath;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.nio.file.*;
//import java.util.List;
//
//@Service
//public class PatchService_old {
//    private static final int MIN_ZOOM = 4;
//    private static final int MAX_ZOOM = 16;
//    private static final int PATCH_TILESERVER_PORT = 8091;
//
//    private final Path projectRoot = Paths.get("").toAbsolutePath();
//
//    private void log(Path logFile, String message) {
//        try {
//            Files.createDirectories(logFile.getParent());
//            Files.writeString(
//                    logFile,
//                    java.time.LocalDateTime.now() + " " + message + System.lineSeparator(),
//                    StandardOpenOption.CREATE,
//                    StandardOpenOption.APPEND
//            );
//        } catch (IOException e) {
//            throw new RuntimeException("Cannot write log file: " + logFile, e);
//        }
//    }
//
//    private int normalizeMinZoom(int minZoom) {
//        return minZoom <= 0 ? MIN_ZOOM : Math.max(MIN_ZOOM, minZoom);
//    }
//
//    private int normalizeMaxZoom(int maxZoom) {
//        return maxZoom <= 0 ? MAX_ZOOM : Math.min(MAX_ZOOM, maxZoom);
//    }
//
//    private String patchStyleJson() {
//        return """
//                {
//                  "version": 8,
//                  "name": "Patch Style",
//                  "center": [105.85, 21.03],
//                  "zoom": 12,
//                  "sources": {
//                    "patch": {
//                      "type": "vector",
//                      "tiles": ["http://localhost:8080/data/patch/{z}/{x}/{y}.pbf"],
//                      "minzoom": 4,
//                      "maxzoom": 16
//                    }
//                  },
//                  "layers": [
//                    {"id":"background","type":"background","paint":{"background-color":"#f3f3f3"}},
//                    {
//                      "id":"water","type":"fill","source":"patch","source-layer":"water",
//                      "minzoom":4,"paint":{"fill-color":"#9fd3ff","fill-opacity":0.85}
//                    },
//                    {
//                      "id":"water-lines","type":"line","source":"patch","source-layer":"water_lines",
//                      "minzoom":4,
//                      "paint":{
//                        "line-color":"#5dade2",
//                        "line-width":["interpolate",["linear"],["zoom"],4,0.3,10,1.2,14,3,16,5]
//                      }
//                    },
//                    {
//                      "id":"water-labels",
//                      "type":"symbol",
//                      "source":"patch",
//                      "source-layer":"water_lines",
//                      "minzoom":4,
//                      "layout":{
//                        "symbol-placement":"line",
//                        "text-field":["get","name"],
//                        "text-size":["interpolate",["linear"],["zoom"],4,8,14,11,16,13]
//                      },
//                      "paint":{
//                        "text-color":"#2e86c1",
//                        "text-halo-color":"#ffffff",
//                        "text-halo-width":1
//                      }
//                    },
//                    {"id":"landuse","type":"fill","source":"patch","source-layer":"landuse","minzoom":4,"paint":{"fill-color":"#d8f0d2","fill-opacity":0.6}},
//                    {"id":"buildings","type":"fill","source":"patch","source-layer":"buildings","minzoom":4,"paint":{"fill-color":"#d9d0c9","fill-outline-color":"#b8aea6","fill-opacity":0.8}},
//                    {"id":"planned-roads","type":"line","source":"patch","source-layer":"planned_roads","minzoom":4,"paint":{"line-color":"#777777","line-width":["interpolate",["linear"],["zoom"],4,0.3,10,0.7,14,3,16,5],"line-dasharray":[2,2],"line-opacity":0.55}},
//                    {"id":"roads-minor","type":"line","source":"patch","source-layer":"roads","minzoom":4,"filter":["in",["get","class"],["literal",["residential","living_street","service","unclassified","track","path","footway","pedestrian","cycleway"]]],"paint":{"line-color":"#9a9a9a","line-width":["interpolate",["linear"],["zoom"],4,0.15,10,0.5,14,2,16,4],"line-opacity":0.75}},
//                    {"id":"roads-major","type":"line","source":"patch","source-layer":"roads","minzoom":4,"filter":["in",["get","class"],["literal",["motorway","trunk","primary","secondary","tertiary"]]],"paint":{"line-color":["match",["get","class"],"motorway","#e63946","trunk","#f77f00","primary","#fcbf49","secondary","#ffd166","tertiary","#ffe599","#555555"],"line-width":["interpolate",["linear"],["zoom"],4,0.3,10,1.2,14,4,16,6]}},
//                    {"id":"road-labels","type":"symbol","source":"patch","source-layer":"roads","minzoom":4,"layout":{"symbol-placement":"line","text-field":["get","name"],"text-size":["interpolate",["linear"],["zoom"],4,7,14,11,16,13],"text-font":["Open Sans Regular"]},"paint":{"text-color":"#333333","text-halo-color":"#ffffff","text-halo-width":1}},
//                    {"id":"poi-labels","type":"symbol","source":"patch","source-layer":"poi","minzoom":4,"layout":{"text-field":["get","name"],"text-size":["interpolate",["linear"],["zoom"],4,7,14,10,16,12]},"paint":{"text-color":"#444444","text-halo-color":"#ffffff","text-halo-width":1}}
//                  ]
//                }
//                """;
//    }
//
//    public String runPatch(String bbox, String patchName, int minZoom, int maxZoom) throws IOException, InterruptedException {
//        int zMin = normalizeMinZoom(minZoom);
//        int zMax = normalizeMaxZoom(maxZoom);
//
//        Path patchDir = projectRoot.resolve("patches").resolve(patchName);
//        if (Files.exists(patchDir)) {
//            deleteDirectory(patchDir);
//        }
//        Files.createDirectories(patchDir);
//
//        Path logFile = patchDir.resolve("patch.log");
//        Path patchPbf = patchDir.resolve(patchName + ".osm.pbf");
//        Path patchMbtiles = patchDir.resolve(patchName + ".mbtiles");
//        Path pngDir = patchDir.resolve("png_tiles");
//
//        log(logFile, "===== START PATCH " + patchName + " =====");
//        log(logFile, "Project root: " + projectRoot);
//        log(logFile, "BBOX: " + bbox);
//        log(logFile, "Zoom: " + minZoom + "-" + maxZoom);
//
//        runCommand(List.of(
//                "osmium", "extract",
//                "--strategy", "complete_ways",
//                "-b", bbox,
//                "data/vietnam-latest.osm.pbf",
//                "-o", patchPbf.toString(),
//                "--overwrite"
//        ), logFile);
//
//        Files.deleteIfExists(patchMbtiles);
//
//        runCommand(List.of(
//                "docker", "run", "--rm",
//                "-v", projectRoot + ":/data",
//                "ghcr.io/systemed/tilemaker:master",
//                "/data/" + projectRoot.relativize(patchPbf),
//                "--output", "/data/" + projectRoot.relativize(patchMbtiles),
//                "--config", "/data/tilemaker/config.json",
//                "--process", "/data/tilemaker/process.lua"
//        ), logFile);
//
//        Process patchTileServer = null;
//        try {
//            patchTileServer = startPatchTileServer(patchDir, patchMbtiles, logFile);
//            renderPngTiles(bbox, minZoom, maxZoom, pngDir, logFile);
//        } finally {
//            if (patchTileServer != null) {
//                log(logFile, "Stopping temporary Patch TileServer...");
//                patchTileServer.destroy();
//                if (patchTileServer.isAlive()) {
//                    patchTileServer.destroyForcibly();
//                }
//            }
//        }
//
//        log(logFile, "===== PATCH DONE =====");
//        return patchDir.toString();
//    }
//
//    private void renderPngTiles(String bbox, int minZoom, int maxZoom, Path outputDir, Path logFile) throws IOException, InterruptedException {
//        Files.createDirectories(outputDir);
//        List<TileMath.Tile> tiles = TileMath.tilesForBbox(bbox, minZoom, maxZoom);
//        log(logFile, "Tiles to render: " + tiles.size());
//
//        for (TileMath.Tile tile : tiles) {
//            Path zxyDir = outputDir.resolve(String.valueOf(tile.z())).resolve(String.valueOf(tile.x()));
//            Files.createDirectories(zxyDir);
//            Path outFile = zxyDir.resolve(tile.y() + ".png");
//            String url = "http://localhost:" + PATCH_TILESERVER_PORT + "/styles/patch/" + tile.z() + "/" + tile.x() + "/" + tile.y() + ".png";
//            runCommand(List.of("curl", "-sS", "-f", url, "-o", outFile.toString()), logFile);
//        }
//    }
//
//    private Process startPatchTileServer(Path patchDir, Path patchMbtiles, Path logFile) throws IOException, InterruptedException {
//
//        Path tileServerDir = patchDir.resolve("tileserver");
//        Path stylesDir = tileServerDir.resolve("styles");
//
//        Files.createDirectories(stylesDir);
//
//        Path configPath = tileServerDir.resolve("config.json");
//        Path stylePath = stylesDir.resolve("patch-style.json");
//
//        Files.writeString(configPath, """
//        {
//          "options": {
//            "paths": {
//              "root": "/config",
//              "styles": "styles",
//              "mbtiles": "/data"
//            }
//          },
//          "styles": {
//            "patch": {
//              "style": "patch-style.json",
//              "serve_rendered": true,
//              "serve_data": true
//            }
//          },
//          "data": {
//            "patch": {
//              "mbtiles": "patch.mbtiles"
//            }
//          }
//        }
//        """);
//
//        Files.writeString(stylePath, patchStyleJson());
//
//        log(logFile, "Starting temporary Patch TileServer on port " + PATCH_TILESERVER_PORT + "...");
//
//        stopExistingPatchTileServer(logFile);
//
//        ProcessBuilder pb = new ProcessBuilder(
//                "docker", "run", "--rm",
//                "-v", patchMbtiles.toAbsolutePath() + ":/data/patch.mbtiles",
//                "-v", tileServerDir.toAbsolutePath() + ":/config",
//                "-p", PATCH_TILESERVER_PORT + ":8080",
//                "maptiler/tileserver-gl",
//                "--config", "/config/config.json"
//        );
//
//        pb.directory(projectRoot.toFile());
//        pb.redirectErrorStream(true);
//        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
//
//        Process process = pb.start();
//        waitForPatchTileServer(logFile);
//
//        return process;
//    }
//
//    private void waitForPatchTileServer(Path logFile) throws InterruptedException {
//        int maxAttempts = 60;
//
//        for (int i = 0; i < maxAttempts; i++) {
//            try {
//                ProcessBuilder pb = new ProcessBuilder(
//                        "curl", "-s", "-f",
//                        "http://localhost:8091/styles/patch/style.json"
//                );
//                Process p = pb.start();
//                int exit = p.waitFor();
//                if (exit == 0) {
//                    log(logFile, "Patch TileServer is ready.");
//                    Thread.sleep(1000);
//                    return;
//                }
//            } catch (Exception ignored) {
//            }
//            Thread.sleep(1000);
//        }
//        throw new RuntimeException("Patch TileServer did not start on port " + PATCH_TILESERVER_PORT);
//    }
//
//    private void runCommand(List<String> command, Path logFile) throws IOException, InterruptedException {
//        log(logFile, "$ " + String.join(" ", command));
//
//        ProcessBuilder pb = new ProcessBuilder(command);
//        pb.directory(projectRoot.toFile());
//        pb.redirectErrorStream(true);
//        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
//
//        Process process = pb.start();
//        int exit = process.waitFor();
//        if (exit != 0) {
//            throw new RuntimeException("Command failed: " + String.join(" ", command) + ". See log: " + logFile);
//        }
//    }
//
//    private void deleteDirectory(Path dir) throws IOException {
//        if (!Files.exists(dir)) return;
//
//        try (var paths = Files.walk(dir)) {
//            paths
//                    .sorted((a, b) -> b.compareTo(a))
//                    .forEach(path -> {
//                        try {
//                            Files.deleteIfExists(path);
//                        } catch (IOException e) {
//                            throw new RuntimeException("Failed to delete: " + path, e);
//                        }
//                    });
//        }
//    }
//
//    private void stopExistingPatchTileServer(Path logFile) {
//        try {
//            runCommand(List.of(
//                    "bash", "-lc",
//                    "docker ps --filter publish=8091 -q | xargs -r docker stop"
//            ), logFile);
//        } catch (Exception e) {
//            log(logFile, "WARN: Could not stop existing patch TileServer: " + e.getMessage());
//        }
//    }
//
//}