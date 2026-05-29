package gis.gis_demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
public class CommandRunnerService {

    private final Path projectRoot = Paths.get("").toAbsolutePath();

    public void runCommand(List<String> command, Path logFile) throws IOException, InterruptedException {
        LogService.log(logFile, "$ " + String.join(" ", command));

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

    public void restartTileServerContainer(Path logFile) throws IOException, InterruptedException {
        LogService.log(logFile, "Restarting main TileServer container on port 8081...");
        runCommand(List.of(
                "bash", "-lc",
                "docker ps --filter publish=8081 -q | xargs -r docker restart"), logFile);
        LogService.log(logFile, "TileServer restart requested.");
    }
}
