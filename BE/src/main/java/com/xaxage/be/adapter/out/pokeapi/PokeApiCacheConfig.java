package com.xaxage.be.adapter.out.pokeapi;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * PokeAPI responses are reused, in memory, for 24 hours after they were fetched.
 */
@Configuration
@EnableCaching
class PokeApiCacheConfig {

    static final String POKEMON_PAGES = "pokeapi-pokemon-pages";
    static final String POKEMON_DETAILS = "pokeapi-pokemon-details";
    static final String NEWEST_VERSION_GROUP = "pokeapi-newest-version-group";

    private static final Duration TIME_TO_LIVE = Duration.ofHours(24);

    @Bean
    Ticker pokeApiCacheTicker() {
        return Ticker.systemTicker();
    }

    @Bean
    CacheManager pokeApiCacheManager(Ticker ticker) {
        var cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(TIME_TO_LIVE).ticker(ticker));
        return cacheManager;
    }
}
