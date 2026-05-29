package gis.gis_demo.service;

import gis.gis_demo.service.LogService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileFolderService {
    private static final Path projectRoot = Paths.get("").toAbsolutePath();

    public static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir))
            return;

        try (var paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete: " + path, e);
                        }
                    });
        }
    }

    public static void backupMainMbtiles(Path mainMbtiles, Path patchDir, Path logFile) throws IOException {
        Path backupDir = projectRoot.resolve("output").resolve("backups");
        Files.createDirectories(backupDir);

        String backupName = "vietnam_before_" + patchDir.getFileName() + ".mbtiles";
        Path backupPath = backupDir.resolve(backupName);

        LogService.log(logFile, "Backing up main MBTiles...");
        LogService.log(logFile, "Backup: " + backupPath);

        Files.copy(mainMbtiles, backupPath, StandardCopyOption.REPLACE_EXISTING);
    }
}

