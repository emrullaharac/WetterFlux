package dev.emrullaharac.wetterflux.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenMeteoClient {

    private final WebClient.Builder webClientBuilder;
    private final MeterRegistry meterRegistry;

    private Counter openMeteoCallsTotal;

    @PostConstruct
    void initMetrics() {
        openMeteoCallsTotal = meterRegistry.counter("openmeteo_calls_total");
    }

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

        log.debug("openmeteo_request uri={}", url);
        openMeteoCallsTotal.increment();

        WebClient client = webClientBuilder.build();

        try {
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
        } catch (RuntimeException ex) {
            log.error("openmeteo_error uri={} message={}", url, ex.getMessage(), ex);
            throw ex;
        }
    }

    private static <T> Optional<T> opt(T v) {
        return Optional.ofNullable(v);
    }
}
