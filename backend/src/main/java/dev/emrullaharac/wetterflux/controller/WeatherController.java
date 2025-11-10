package dev.emrullaharac.wetterflux.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    @GetMapping("api/health")
    public String health() {
        return "OK";
    }
}
