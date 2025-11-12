package dev.emrullaharac.wetterflux.model.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class WeatherResponseDto {

    double latitude;
    double longitude;
    String timezone;
    String timezoneAbbreviation;
    Double currentTemperature2m;
    Double currentWindSpeed10m;
    Integer currentWeatherCode;
    String description;
    String currentTimeIso;
    List<String> hourlyTime;
    Map<String, List<Double>> hourlyValues;
    Map<String, List<String>> hourlyUnits;

}
