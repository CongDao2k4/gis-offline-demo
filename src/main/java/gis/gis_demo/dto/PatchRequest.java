package gis.gis_demo.dto;

public record PatchRequest(
        String bbox,
        String patchName,
        int minZoom,
        int maxZoom
) {}
