package gis.gis_demo.service;

import gis.gis_demo.dto.DiffItem;
import gis.gis_demo.repository.CompareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompareService {

    private final CompareRepository compareRepository;

    public List<DiffItem> compareBbox(double minLon, double minLat, double maxLon, double maxLat) {
        List<DiffItem> results = new ArrayList<>();
        
        // Define BBOX CTE string for reuse
        String bboxStr = String.format(java.util.Locale.US, "ST_Transform(ST_MakeEnvelope(%f, %f, %f, %f, 4326), 3857)", minLon, minLat, maxLon, maxLat);

        // 1. CREATIONS (NEW)
        results.addAll(compareRepository.findNewPoints(bboxStr));
        results.addAll(compareRepository.findNewLines(bboxStr));
        results.addAll(compareRepository.findNewPolygons(bboxStr));

        // 2. MODIFICATIONS
        results.addAll(compareRepository.findModifiedLines(bboxStr));
        results.addAll(compareRepository.findModifiedPolygons(bboxStr));

        // 3. DELETIONS : Deleted Lines
        results.addAll(compareRepository.findDeletedLines(bboxStr));

        return results;
    }
}
