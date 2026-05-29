package gis.gis_demo.service;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
public class LogService {
    public static void log(Path logFile, String message) {
        // Ghi ra Console bằng SLF4J
        log.info(message);
        
        // Ghi song song ra file patch.log
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(
                    logFile,
                    java.time.LocalDateTime.now() + " " + message + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Cannot write log file: {}", logFile, e);
            throw new RuntimeException("Cannot write log file: " + logFile, e);
        }
    }
}
