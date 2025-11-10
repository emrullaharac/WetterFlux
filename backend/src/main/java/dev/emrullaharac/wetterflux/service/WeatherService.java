package dev.emrullaharac.wetterflux.service;

import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    public String getDummyWeather() {
        return "Sunny 20°C (dummy)";
    }
}
