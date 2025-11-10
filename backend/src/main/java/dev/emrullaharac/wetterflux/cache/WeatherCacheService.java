package dev.emrullaharac.wetterflux.cache;

import org.springframework.stereotype.Service;

@Service
public class WeatherCacheService {

    public String getFromCache(double lat, double lon) {
        return null; // no cache yet
    }

    public void putToCache(double lat, double lon) {
        // no-op for now
    }
}
