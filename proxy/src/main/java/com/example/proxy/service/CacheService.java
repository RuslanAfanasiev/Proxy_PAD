package com.example.proxy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * CacheService handles caching of responses using Redis.
 * Implements temporary storage of responses to reduce load on Data Warehouse nodes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${proxy.cache.ttl:300000}")
    private long cacheTtl;

    /**
     * Store response in cache with TTL.
     *
     * @param key Cache key (usually the request URL and method)
     * @param value Response object to cache
     */
    public void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value, cacheTtl, TimeUnit.MILLISECONDS);
            log.debug("Cached response for key: {}", key);
        } catch (Exception e) {
            log.error("Error caching response for key: {}", key, e);
        }
    }

    /**
     * Retrieve cached response.
     *
     * @param key Cache key
     * @return Cached object or null if not found
     */
    public Object get(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache HIT for key: {}", key);
            } else {
                log.debug("Cache MISS for key: {}", key);
            }
            return cached;
        } catch (Exception e) {
            log.error("Error retrieving cached response for key: {}", key, e);
            return null;
        }
    }

    /**
     * Invalidate cache for specific key.
     *
     * @param key Cache key to invalidate
     */
    public void invalidate(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Invalidated cache for key: {}", key);
        } catch (Exception e) {
            log.error("Error invalidating cache for key: {}", key, e);
        }
    }

    /**
     * Generate cache key from request parameters.
     *
     * @param method HTTP method
     * @param path Request path
     * @return Cache key
     */
    public String generateKey(String method, String path) {
        return String.format("%s:%s", method, path);
    }

    /**
     * Check if key exists in cache.
     *
     * @param key Cache key
     * @return true if key exists, false otherwise
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking cache key: {}", key, e);
            return false;
        }
    }
}
