package dev.emrullaharac.wetterflux.cache;

import dev.emrullaharac.wetterflux.model.dto.WeatherResponseDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherCacheService {

    private final RedisTemplate<String, WeatherResponseDto> weatherRedisTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${weather.cache.ttl-minutes:15}")
    private long ttlMinutes;

    private Counter cacheHits;
    private Counter cacheMisses;

    @PostConstruct
    void initMetrics() {
        cacheHits = meterRegistry.counter("weather_cache_hits_total");
        cacheMisses = meterRegistry.counter("weather_cache_misses_total");
    }

    private String nullSafe(Object o) {
        return o == null ? "null" : o.toString();
    }

    private String buildKey(
            double latitude,
            double longitude,
            String currentVars,
            String hourlyVars,
            String dailyVars,
            Integer forecastDays,
            String timezone,
            String temperatureUnit,
            String windSpeedUnit
    ) {
        return "weather:%s:%s:%s:%s:%s:%s:%s:%s:%s".formatted(
                latitude,
                longitude,
                nullSafe(currentVars),
                nullSafe(hourlyVars),
                nullSafe(dailyVars),
                nullSafe(forecastDays),
                nullSafe(timezone),
                nullSafe(temperatureUnit),
                nullSafe(windSpeedUnit)
        );
    }

    public Optional<WeatherResponseDto> getFromCache(
            double latitude,
            double longitude,
            String currentVars,
            String hourlyVars,
            String dailyVars,
            Integer forecastDays,
            String timezone,
            String temperatureUnit,
            String windSpeedUnit
    ) {
        String key = buildKey(latitude, longitude, currentVars, hourlyVars, dailyVars,
                forecastDays, timezone, temperatureUnit, windSpeedUnit);

        log.debug("cache_get keyHash={}", key.hashCode());

        WeatherResponseDto fromRedis = weatherRedisTemplate.opsForValue().get(key);

        if (fromRedis != null) {
            cacheHits.increment();
            return Optional.of(fromRedis);
        } else {
            cacheMisses.increment();
            return Optional.empty();
        }
    }

    public void putToCache(
            double latitude,
            double longitude,
            String currentVars,
            String hourlyVars,
            String dailyVars,
            Integer forecastDays,
            String timezone,
            String temperatureUnit,
            String windSpeedUnit,
            WeatherResponseDto responseDto
    ) {
        String key = buildKey(latitude, longitude, currentVars, hourlyVars, dailyVars,
                forecastDays, timezone, temperatureUnit, windSpeedUnit);

        log.debug("cache_put keyHash={}", key.hashCode());

        weatherRedisTemplate.opsForValue()
                .set(key, responseDto, Duration.ofMinutes(ttlMinutes));
    }
}
