package gis.gis_demo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AppConfig {

    @Value("${app.map.source-pbf:data/vietnam-latest.osm.pbf}")
    private String sourcePbf;

    @Value("${app.map.base-pbf:data/vietnam-180101.osm.pbf}")
    private String basePbf;

    @Value("${app.map.output-mbtiles:output/vietnam.mbtiles}")
    private String outputMbtiles;

    @Value("${app.map.min-zoom:4}")
    private int minZoom;

    @Value("${app.map.max-zoom:16}")
    private int maxZoom;

    @Value("${app.map.style-name:vietnam}")
    private String styleName;

    @Value("${app.tileserver.host:localhost}")
    private String tileserverHost;

    @Value("${app.tileserver.port:8081}")
    private int tileserverPort;
}
