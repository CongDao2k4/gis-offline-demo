package gis.gis_demo.dto;

/**
 * @param id : Id of osm object in DB
 * @param type : Type of facility such as Point, Line, Polygon
 * @param changeType : Compare to older version to mark that what object had been changed in the latest version of OSM.PBF
 * @param name : Name of facility or object is detected
 * @param geojson : Value of function ST_AsGeoJSON in SQL PostGres
 */

public record DiffItem(
        String id,
        String type, // POINT, LINE, POLYGON
        String changeType, // NEW, DELETED, MODIFIED
        String name,
        String geojson // ST_AsGeoJSON result
) {}
