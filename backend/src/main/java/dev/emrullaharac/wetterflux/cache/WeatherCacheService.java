package dev.emrullaharac.wetterflux.cache;

import dev.emrullaharac.wetterflux.model.dto.WeatherResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeatherCacheService {

    private final RedisTemplate<String, WeatherResponseDto> weatherRedisTemplate;

    @Value("${weather.cache.ttl-minutes:15}")
    private long ttlMinutes;

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

        WeatherResponseDto dto = weatherRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(dto);
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

        weatherRedisTemplate.opsForValue()
                .set(key, responseDto, Duration.ofMinutes(ttlMinutes));
    }
}
