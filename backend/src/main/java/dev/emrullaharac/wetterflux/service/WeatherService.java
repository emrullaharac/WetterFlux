package dev.emrullaharac.wetterflux.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.emrullaharac.wetterflux.client.OpenMeteoClient;
import dev.emrullaharac.wetterflux.exception.ApiException;
import dev.emrullaharac.wetterflux.model.dto.WeatherResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final OpenMeteoClient openMeteoClient;

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
        JsonNode root = openMeteoClient.fetchForecast(lat, lon, currentVars, hourlyVars, dailyVars,
                forecastDays, timezone, temperatureUnit, windSpeedUnit);
        if (root == null || root.isMissingNode()) {
            throw new ApiException("Open-Meteo empty response");
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

        return WeatherResponseDto.builder()
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
