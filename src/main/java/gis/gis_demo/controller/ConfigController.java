package gis.gis_demo.controller;

import gis.gis_demo.config.AppConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final AppConfig appConfig;

    public ConfigController(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @GetMapping
    public AppConfig getConfig() {
        return appConfig;
    }
}
