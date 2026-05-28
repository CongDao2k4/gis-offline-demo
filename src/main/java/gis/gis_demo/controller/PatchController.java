package gis.gis_demo.controller;

import gis.gis_demo.dto.PatchRequest;
import gis.gis_demo.service.PatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patch")
public class PatchController {
    private final PatchService patchService;

    @Autowired
    public PatchController(PatchService patchService) {
        this.patchService = patchService;
    }

    @PostMapping
    public Map<String, Object> createPatch(@RequestBody PatchRequest request) throws Exception {
        String outputDir = patchService.runPatch( request.bbox(), request.patchName(), request.minZoom(), request.maxZoom() );

        return Map.of(
                "status", "ok",
                "patchName", request.patchName(),
                "outputDir", outputDir
        );
    }
}