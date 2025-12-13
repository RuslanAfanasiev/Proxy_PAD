package com.example.proxy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final long DEFAULT_TTL = 60; // 60 seconds

    /**
     * Get a cached response from Redis.
     *
     * @param key the cache key
     * @return the cached response or null if not found
     */
    public String get(String key) {
        try {
            String cachedValue = redisTemplate.opsForValue().get(key);
            if (cachedValue != null) {
                log.info("Cache HIT for key: {}", key);
                return cachedValue;
            } else {
                log.info("Cache MISS for key: {}", key);
                return null;
            }
        } catch (Exception e) {
            log.error("Error retrieving from cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Store a response in Redis cache with default TTL.
     *
     * @param key the cache key
     * @param value the value to cache
     */
    public void put(String key, String value) {
        put(key, value, DEFAULT_TTL);
    }

    /**
     * Store a response in Redis cache with custom TTL.
     *
     * @param key the cache key
     * @param value the value to cache
     * @param ttlSeconds time to live in seconds
     */
    public void put(String key, String value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            log.info("Cached response for key: {} with TTL: {}s", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Error storing to cache: {}", e.getMessage());
        }
    }

    /**
     * Invalidate a cache entry.
     *
     * @param key the cache key to invalidate
     */
    public void invalidate(String key) {
        try {
            redisTemplate.delete(key);
            log.info("Invalidated cache for key: {}", key);
        } catch (Exception e) {
            log.error("Error invalidating cache: {}", e.getMessage());
        }
    }

    /**
     * Invalidate all cache entries matching a pattern.
     *
     * @param pattern the pattern to match (e.g., "movies:*")
     */
    public void invalidatePattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Invalidated {} cache entries matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Error invalidating cache pattern: {}", e.getMessage());
        }
    }

    /**
     * Generate a cache key from request path and parameters.
     *
     * @param path the request path
     * @param params optional parameters
     * @return the cache key
     */
    public String generateKey(String path, String... params) {
        StringBuilder key = new StringBuilder(path);
        for (String param : params) {
            key.append(":").append(param);
        }
        return key.toString();
    }
}
