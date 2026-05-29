package gis.gis_demo.controller;

import gis.gis_demo.dto.DiffItem;
import gis.gis_demo.service.CompareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/compare")
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;

    @GetMapping
    public List<DiffItem> compare(@RequestParam("bbox") String bbox) {
        String[] parts = bbox.split(",");
        double minLon = Double.parseDouble(parts[0]);
        double minLat = Double.parseDouble(parts[1]);
        double maxLon = Double.parseDouble(parts[2]);
        double maxLat = Double.parseDouble(parts[3]);
        
        return compareService.compareBbox(minLon, minLat, maxLon, maxLat);
    }
}
