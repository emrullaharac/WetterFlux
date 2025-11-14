package dev.emrullaharac.wetterflux.controller;

import dev.emrullaharac.wetterflux.model.dto.WeatherResponseDto;
import dev.emrullaharac.wetterflux.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<WeatherResponseDto> getWeather(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(value = "current", required = false) String currentVars,
            @RequestParam(value = "hourly", required = false) String hourlyVars,
            @RequestParam(value = "daily", required = false) String dailyVars,
            @RequestParam(value = "forecast_days", required = false) Integer forecastDays,
            @RequestParam(value = "timezone", required = false, defaultValue = "auto") String timezone,
            @RequestParam(value = "temperature_unit", required = false) String temperatureUnit,
            @RequestParam(value = "wind_speed_unit", required = false) String windSpeedUnit
    ) {
        log.info(
                "weather_request lat={} lon={} forecastDays={} timezone={} tempUnit={} windUnit={}",
                lat, lon, forecastDays, timezone, temperatureUnit, windSpeedUnit
        );
        return ResponseEntity.ok(weatherService.getWeather(
                lat, lon, currentVars, hourlyVars, dailyVars,forecastDays, timezone,
                temperatureUnit, windSpeedUnit));
    }
}
