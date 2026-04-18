package com.dev.lib.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class ApplicationCacheConfiguration implements CachingConfigurer {

    @Bean("applicationCacheManager")
    @Primary
    public CacheManager applicationCacheManager() {

        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.setCaffeine(Caffeine.newBuilder().expireAfter(new Expiry<>() {
            @Override
            public long expireAfterCreate(@NonNull Object key, @NonNull Object value, long currentTime) {

                long jitter = ThreadLocalRandom.current().nextLong(-2, 3);
                return Duration.ofMinutes(10 + jitter).toNanos();
            }

            @Override
            public long expireAfterUpdate(@NonNull Object key, @NonNull Object value, long currentTime, long currentDuration) {

                return currentDuration;
            }

            @Override
            public long expireAfterRead(@NonNull Object key, @NonNull Object value, long currentTime, long currentDuration) {

                return currentDuration;
            }
        }));

        return manager;
    }

    @Override
    public CacheManager cacheManager() {

        return applicationCacheManager();
    }

}
