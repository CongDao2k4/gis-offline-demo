package gis.gis_demo.repository;

import gis.gis_demo.dto.DiffItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CompareRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<DiffItem> findNewPoints(String bboxStr) {
        String sql = """
            SELECT n.osm_id, n.name, ST_AsGeoJSON(ST_Transform(n.way, 4326)) as geom
            FROM vnew_point n
            LEFT JOIN v2018_point o ON n.osm_id = o.osm_id
            WHERE o.osm_id IS NULL
              AND ST_Intersects(n.way, %1$s)
            LIMIT 100
        """.formatted(bboxStr);
        return fetchDiffs(sql, "POINT", "NEW");
    }

    public List<DiffItem> findNewLines(String bboxStr) {
        String sql = """
            SELECT n.osm_id, n.name, ST_AsGeoJSON(ST_Transform(ST_Intersection(n.way, %1$s), 4326)) as geom
            FROM vnew_line n
            LEFT JOIN v2018_line o ON n.osm_id = o.osm_id
            WHERE o.osm_id IS NULL
              AND ST_Intersects(n.way, %1$s)
            LIMIT 100
        """.formatted(bboxStr);
        return fetchDiffs(sql, "LINE", "NEW");
    }

    public List<DiffItem> findNewPolygons(String bboxStr) {
        String sql = """
            SELECT n.osm_id, n.name, ST_AsGeoJSON(ST_Transform(ST_Intersection(n.way, %1$s), 4326)) as geom
            FROM vnew_polygon n
            LEFT JOIN v2018_polygon o ON n.osm_id = o.osm_id
            WHERE o.osm_id IS NULL
              AND ST_Intersects(n.way, %1$s)
            LIMIT 100
        """.formatted(bboxStr);
        return fetchDiffs(sql, "POLYGON", "NEW");
    }

    public List<DiffItem> findModifiedLines(String bboxStr) {
        String sql = """
            SELECT n.osm_id, n.name, ST_AsGeoJSON(ST_Transform(ST_Intersection(n.way, %1$s), 4326)) as geom
            FROM vnew_line n
            JOIN v2018_line o ON n.osm_id = o.osm_id
            WHERE ST_Intersects(n.way, %1$s)
              AND (NOT ST_Equals(ST_Intersection(o.way, %1$s), ST_Intersection(n.way, %1$s)) OR o.name != n.name OR o.highway != n.highway)
            LIMIT 100
        """.formatted(bboxStr);
        return fetchDiffs(sql, "LINE", "MODIFIED");
    }

    public List<DiffItem> findModifiedPolygons(String bboxStr) {
        String sql = """
            SELECT n.osm_id, n.name, ST_AsGeoJSON(ST_Transform(ST_Intersection(n.way, %1$s), 4326)) as geom
            FROM vnew_polygon n
            JOIN v2018_polygon o ON n.osm_id = o.osm_id
            WHERE ST_Intersects(n.way, %1$s)
              AND (NOT ST_Equals(ST_Intersection(o.way, %1$s), ST_Intersection(n.way, %1$s)) OR o.name != n.name)
            LIMIT 100
        """.formatted(bboxStr);
        return fetchDiffs(sql, "POLYGON", "MODIFIED");
    }

    public List<DiffItem> findDeletedLines(String bboxStr) {
        String sql = """
            SELECT o.osm_id, o.name, ST_AsGeoJSON(ST_Transform(ST_Intersection(o.way, %1$s), 4326)) as geom
            FROM v2018_line o
            LEFT JOIN vnew_line n ON o.osm_id = n.osm_id
            WHERE n.osm_id IS NULL
              AND ST_Intersects(o.way, %1$s)
            LIMIT 100
        """.formatted(bboxStr);
        return fetchDiffs(sql, "LINE", "DELETED");
    }

    private List<DiffItem> fetchDiffs(String sql, String type, String changeType) {
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                String id = rs.getString("osm_id");
                String name = rs.getString("name");
                if (name == null || name.isBlank()) {
                    name = "Unnamed " + type;
                }
                String geom = rs.getString("geom");
                return new DiffItem(id, type, changeType, name, geom);
            });
        } catch (Exception e) {
            log.error("[FAILED] That bai khi query nhung phan khac nhau cho {} / {}: {}", type, changeType, e.getMessage());
            return new ArrayList<>();
        }
    }
}
