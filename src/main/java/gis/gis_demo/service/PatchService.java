package gis.gis_demo.service;

import gis.gis_demo.util.TileMath;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Service
public class PatchService {

    private final Path projectRoot = Paths.get("").toAbsolutePath();

    public String runPatch(String bbox, String patchName, int minZoom, int maxZoom)
            throws IOException, InterruptedException {

        Path patchDir = projectRoot.resolve("patches").resolve(patchName);
        Files.createDirectories(patchDir);

        Path patchPbf = patchDir.resolve(patchName + ".osm.pbf");
        Path patchMbtiles = patchDir.resolve(patchName + ".mbtiles");
        Path pngDir = patchDir.resolve("png_tiles");

        runCommand(List.of(
                "osmium", "extract",
                "--strategy", "complete_ways",
                "-b", bbox,
                "data/vietnam-latest.osm.pbf",
                "-o", patchPbf.toString(),
                "--overwrite"
        ));

        Files.deleteIfExists(patchMbtiles);

        runCommand(List.of(
                "docker", "run", "--rm",
                "-v", projectRoot + ":/data",
                "ghcr.io/systemed/tilemaker:master",
                "/data/" + projectRoot.relativize(patchPbf),
                "--output", "/data/" + projectRoot.relativize(patchMbtiles),
                "--config", "/data/tilemaker/config.json",
                "--process", "/data/tilemaker/process.lua"
        ));

        renderPngTiles(bbox, minZoom, maxZoom, pngDir);

        return patchDir.toString();
    }

    private void renderPngTiles(String bbox, int minZoom, int maxZoom, Path outputDir)
            throws IOException, InterruptedException {

        Files.createDirectories(outputDir);

        List<TileMath.Tile> tiles = TileMath.tilesForBbox(bbox, minZoom, maxZoom);

        for (TileMath.Tile tile : tiles) {
            Path zxyDir = outputDir
                    .resolve(String.valueOf(tile.z()))
                    .resolve(String.valueOf(tile.x()));

            Files.createDirectories(zxyDir);

            Path outFile = zxyDir.resolve(tile.y() + ".png");

            String url = "http://localhost:8081/styles/hanoi/"
                    + tile.z() + "/"
                    + tile.x() + "/"
                    + tile.y() + ".png";

            runCommand(List.of(
                    "curl", "-sS",
                    url,
                    "-o", outFile.toString()
            ));
        }
    }

    private void runCommand(List<String> command)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectRoot.toFile());
        pb.inheritIO();

        Process process = pb.start();
        int exit = process.waitFor();

        if (exit != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command));
        }
    }
}