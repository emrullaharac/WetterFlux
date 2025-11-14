package dev.emrullaharac.wetterflux.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.emrullaharac.wetterflux.cache.WeatherCacheService;
import dev.emrullaharac.wetterflux.client.OpenMeteoClient;
import dev.emrullaharac.wetterflux.exception.ApiException;
import dev.emrullaharac.wetterflux.model.dto.WeatherResponseDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final OpenMeteoClient openMeteoClient;
    private final WeatherCacheService  weatherCacheService;
    private final MeterRegistry meterRegistry;

    private Counter weatherRequestsTotal;
    private Timer weatherRequestsDuration;

    @PostConstruct
    void initMetrics() {
        weatherRequestsTotal = meterRegistry.counter("weather_requests_total");
        weatherRequestsDuration = meterRegistry.timer("weather_requests_duration");
    }

    public WeatherResponseDto getWeather(
            double lat,
            double lon,
            String currentVars,
            String hourlyVars,
            String dailyVars,
            Integer forecastDays,
            String timezone,
            String temperatureUnit,
            String windSpeedUnit
    ) {
        validateParams(lat, lon, forecastDays, temperatureUnit);

        weatherRequestsTotal.increment();

        return weatherRequestsDuration.record(() -> {
            String effectiveCurrent = (currentVars == null || currentVars.isBlank())
                    ? "temperature_2m,wind_speed_10m,weather_code"
                    : currentVars;

            String effectiveHourly = (hourlyVars == null || hourlyVars.isBlank())
                    ? "temperature_2m,relative_humidity_2m"
                    : hourlyVars;

            String effectiveDaily = (dailyVars == null || dailyVars.isBlank())
                    ? "weather_code,temperature_2m_max,temperature_2m_min"
                    : dailyVars;

            var cached = weatherCacheService.getFromCache(
                    lat, lon,
                    effectiveCurrent,
                    effectiveHourly,
                    effectiveDaily,
                    forecastDays,
                    timezone,
                    temperatureUnit,
                    windSpeedUnit
            );
            if (cached.isPresent()) {
                log.info("cache_hit lat={} lon={} forecastDays={}", lat, lon, forecastDays);
                return cached.get();
            }

            log.info("cache_miss lat={} lon={} forecastDays={}", lat, lon, forecastDays);

            log.info("calling_openmeteo lat={} lon={} forecastDays={} timezone={}",
                    lat, lon, forecastDays, timezone);

            JsonNode root;

            try {
                root = openMeteoClient.fetchForecast(
                        lat, lon,
                        effectiveCurrent,
                        effectiveHourly,
                        effectiveDaily,
                        forecastDays,
                        timezone,
                        temperatureUnit,
                        windSpeedUnit);
            } catch (Exception ex) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to fetch weather data from provider");
            }

            if (root == null || root.isMissingNode()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Empty response from weather provider");
            }

            String tz = text(root, "timezone");
            String tzAbbr = text(root, "timezone_abbreviation");

            // CURRENT
            Double currentTemp = null, currentWind = null;
            Integer currentCode = null;
            String currentTimeIso = null;
            JsonNode current = root.get("current");
            if (current != null && !current.isMissingNode()) {
                currentTemp = number(current, "temperature_2m");
                currentWind = number(current, "wind_speed_10m");
                currentTimeIso =  text(current, "time");
                if (current.hasNonNull("weather_code")) {
                    currentCode = current.get("weather_code").isInt()
                            ? current.get("weather_code").asInt()
                            : null;
                }
            }

            // HOURLY
            List<String> hourlyTime = new ArrayList<>();
            Map<String, List<Double>> hourlyValues = new LinkedHashMap<>();
            Map<String, String> hourlyUnits = new LinkedHashMap<>();

            JsonNode hourly = root.get("hourly");
            JsonNode hourlyUnitsNode = root.get("hourly_units");
            if (hourlyUnitsNode != null && hourlyUnitsNode.isObject()) {
                hourlyUnitsNode.properties().forEach(e ->
                        hourlyUnits.put(e.getKey(), e.getValue().asText()));
            }
            if (hourly != null && hourly.isObject()) {
                JsonNode timeArr = hourly.get("time");
                if (timeArr != null && timeArr.isArray()) {
                    for (JsonNode t : timeArr) hourlyTime.add(t.asText());
                }
                hourly.fieldNames().forEachRemaining(var -> {
                    if ("time".equals(var)) return;
                    JsonNode arr = hourly.get(var);
                    if (arr != null && arr.isArray()) {
                        List<Double> vals = new ArrayList<>(arr.size());
                        for (JsonNode v : arr) vals.add(v.isNumber() ? v.asDouble() : null);
                        hourlyValues.put(var, vals);
                    }
                });
            }

            String description = mapWeatherCodeToText(currentCode);

            WeatherResponseDto response = WeatherResponseDto.builder()
                    .latitude(lat)
                    .longitude(lon)
                    .timezone(tz)
                    .timezoneAbbreviation(tzAbbr)
                    .currentTemperature2m(currentTemp)
                    .currentWindSpeed10m(currentWind)
                    .currentWeatherCode(currentCode)
                    .description(description)
                    .currentTimeIso(currentTimeIso)
                    .hourlyTime(hourlyTime)
                    .hourlyValues(hourlyValues)
                    .hourlyUnits(hourlyUnits)
                    .build();

            weatherCacheService.putToCache(
                    lat, lon,
                    effectiveCurrent,
                    effectiveHourly,
                    effectiveDaily,
                    forecastDays,
                    timezone,
                    temperatureUnit,
                    windSpeedUnit,
                    response
            );
            return response;
        });
    }

    private void validateParams(double lat, double lon, Integer forecastDays, String temperatureUnit) {
        if (lat < -90 || lat > 90) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lat is out of range, must be between -90 and 90.");
        }

        if (lon < -180 || lon > 180) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "lon is out of range, must be between -180 and 180.");
        }

        if (forecastDays != null && (forecastDays < 1 || forecastDays > 16)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "forecastDays must be between 1 and 16.");
        }

        if (temperatureUnit != null
                && !temperatureUnit.equalsIgnoreCase("celsius")
                && !temperatureUnit.equalsIgnoreCase("fahrenheit")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "temperatureUnit must be either 'celsius' or 'fahrenheit'");
        }
    }

    private String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private Double number(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) && node.get(field).isNumber()
                ? node.get(field).asDouble() : null;
    }

    private static String mapWeatherCodeToText(Integer code) {
        if (code == null) {return null;}

        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown";
        };
    }
}
