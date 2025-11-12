package dev.emrullaharac.wetterflux.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OpenMeteoClient {

    private final WebClient.Builder webClientBuilder;

    public JsonNode fetchForecast(
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
        String url = UriComponentsBuilder
                .fromUriString("https://api.open-meteo.com/v1/forecast")
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParamIfPresent("current", opt(currentVars))
                .queryParamIfPresent("hourly", opt(hourlyVars))
                .queryParamIfPresent("daily", opt(dailyVars))
                .queryParamIfPresent("forecast_days",  opt(forecastDays))
                .queryParam("timezone", timezone != null ? timezone : "auto")
                .queryParamIfPresent("temperature_unit", opt(temperatureUnit))
                .queryParamIfPresent("wind_speed_unit", opt(windSpeedUnit))
                .build().toUriString();

        WebClient client = webClientBuilder.build();

        return client.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp ->
                        resp.createException().map(ex ->
                                new RuntimeException("Open-Meteo 4xx: " + ex.getResponseBodyAsString(), ex)))
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.createException().map(ex ->
                                new RuntimeException("Open-Meteo 5xx: " + ex.getResponseBodyAsString(), ex)))
                .bodyToMono(JsonNode.class)
                .block();
    }

    private static <T> Optional<T> opt(T v) {
        return Optional.ofNullable(v);
    }
}
