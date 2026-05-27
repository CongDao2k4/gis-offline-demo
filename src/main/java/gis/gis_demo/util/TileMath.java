package gis.gis_demo.util;

import java.util.ArrayList;
import java.util.List;

public class TileMath {

    public record Tile(int z, int x, int y) {}

    public record BBox(double minLon, double minLat, double maxLon, double maxLat) {
        public static BBox parse(String bbox) {
            String[] p = bbox.split(",");
            if (p.length != 4) {
                throw new IllegalArgumentException("BBox must be minLon,minLat,maxLon,maxLat");
            }
            return new BBox(
                    Double.parseDouble(p[0]),
                    Double.parseDouble(p[1]),
                    Double.parseDouble(p[2]),
                    Double.parseDouble(p[3])
            );
        }
    }

    public static Tile lonLatToTile(double lon, double lat, int z) {
        double latRad = Math.toRadians(lat);
        int n = 1 << z;

        int x = (int) Math.floor((lon + 180.0) / 360.0 * n);
        int y = (int) Math.floor(
                (1.0 - Math.asin(Math.tan(latRad)) / Math.PI) / 2.0 * n
        );

        return new Tile(z, x, y);
    }

    public static List<Tile> tilesForBbox(String bboxText, int minZoom, int maxZoom) {
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