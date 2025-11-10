package dev.emrullaharac.wetterflux.client;

import org.springframework.stereotype.Component;

@Component
public class OpenMeteoClient {

    // TODO: HTTP client implementation will be added later
    public String fetchDummyWeatherData(double lat, double lon) {
        return "raw-open-meteo-response (dummy)";
    }
}
