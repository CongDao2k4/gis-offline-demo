package gis.gis_demo.util;

import java.util.ArrayList;
import java.util.List;

public class TileMath {

    public record Tile(int z, int x, int y) {}

    public record BBox(double minLon, double minLat, double maxLon, double maxLat) {
        public static BBox parse(String bbox) {
            String[] p = bbox.split(",");
            if (p.length != 4) {
                throw new IllegalArgumentException("BBox cần đủ kinh độ trái, vĩ độ dưới, kinh độ phải, vĩ độ trên");
            }
            double minLon = Double.parseDouble(p[0].trim());
            double minLat = Double.parseDouble(p[1].trim());
            double maxLon = Double.parseDouble(p[2].trim());
            double maxLat = Double.parseDouble(p[3].trim());
            if (minLon > maxLon || minLat > maxLat) {
                throw new IllegalArgumentException("Invalid bbox order: min values must be <= max values");
            }
            return new BBox(minLon, minLat, maxLon, maxLat);
        }
    }

    public static Tile lonLatToTile(double lon, double lat, int z) {
        double clampedLat = Math.max(Math.min(lat, 85.05112878), -85.05112878);
        double latRad = Math.toRadians(clampedLat);
        int n = 1 << z; // số nhị phần dịch sang trái 1 vị trí => nhân 2
        int x = (int) Math.floor((lon + 180.0) / 360.0 * n);
        int y = (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n);
        x = Math.max(0, Math.min(n - 1, x));
        y = Math.max(0, Math.min(n - 1, y));
        return new Tile(z, x, y);
    }

    public static List<Tile> tilesForBbox(String bboxText, int minZoom, int maxZoom) {
        if (minZoom < 0 || maxZoom < minZoom || maxZoom > 30) {
            throw new IllegalArgumentException("Invalid zoom range: " + minZoom + "-" + maxZoom);
        }
        BBox b = BBox.parse(bboxText);
        List<Tile> tiles = new ArrayList<>();

        for (int z = minZoom; z <= maxZoom; z++) {
            Tile topLeft = lonLatToTile(b.minLon(), b.maxLat(), z);
            Tile bottomRight = lonLatToTile(b.maxLon(), b.minLat(), z);

            int xmin = Math.min(topLeft.x(), bottomRight.x());
            int xmax = Math.max(topLeft.x(), bottomRight.x());
            int ymin = Math.min(topLeft.y(), bottomRight.y());
            int ymax = Math.max(topLeft.y(), bottomRight.y());

            for (int x = xmin; x <= xmax; x++) {
                for (int y = ymin; y <= ymax; y++) {
                    tiles.add(new Tile(z, x, y));
                }
            }
        }

        return tiles;
    }
}