package dev.emrullaharac.wetterflux.model.dto;

import lombok.*;

@AllArgsConstructor
@With
@Builder
@Getter
public class WeatherResponseDto {

    private double temperature;
    private double windSpeed;
    private String description;
}
