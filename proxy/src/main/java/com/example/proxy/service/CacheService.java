package com.example.proxy.service;

import com.example.proxy.interfaces.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

/**
 * CacheService handles caching of responses using Redis.
 * Implements temporary storage of responses to reduce load on Data Warehouse nodes.
 *
 * If Redis is not available at runtime, this service falls back to an in-memory cache
 * so the proxy remains functional and caching/invalidation can still be tested locally.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService implements ICacheService<String, Object> {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${proxy.cache.ttl:300000}")
    private long cacheTtl;

    private final ConcurrentMap<String, LocalCacheEntry> localCache = new ConcurrentHashMap<>();
    private final AtomicBoolean redisWarningLogged = new AtomicBoolean(false);

    /**
     * Store response in cache with TTL.
     *
     * @param key Cache key (usually the request URL and method)
     * @param value Response object to cache
     */
    @Override
    public void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value, cacheTtl, TimeUnit.MILLISECONDS);
            log.debug("Cached response for key: {}", key);
        } catch (Exception e) {
            logRedisDownOnce(e);
            localPut(key, value);
        }
    }

    /**
     * Retrieve cached response.
     *
     * @param key Cache key
     * @return Cached object or null if not found
     */
    @Override
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
            logRedisDownOnce(e);
            return localGet(key);
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
            logRedisDownOnce(e);
            localInvalidate(key);
        }
    }

    /**
     * Evict cache for specific key (alias for invalidate to implement ICacheService).
     *
     * @param key Cache key to evict
     */
    @Override
    public void evict(String key) {
        invalidate(key);
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
            logRedisDownOnce(e);
            return localHasKey(key);
        }
    }

    private void logRedisDownOnce(Exception e) {
        if (redisWarningLogged.compareAndSet(false, true)) {
            log.warn("Redis is not reachable; falling back to in-memory cache. Cause: {}", e.getMessage());
        }
        log.debug("Redis operation failed; using in-memory cache fallback. Cause:", e);
    }

    private void localPut(String key, Object value) {
        localCache.put(key, new LocalCacheEntry(value, System.currentTimeMillis() + cacheTtl));
        log.debug("Cached response in-memory for key: {}", key);
    }

    private Object localGet(String key) {
        LocalCacheEntry entry = localCache.get(key);
        if (entry == null) {
            log.debug("In-memory cache MISS for key: {}", key);
            return null;
        }
        if (entry.isExpired()) {
            localCache.remove(key, entry);
            log.debug("In-memory cache EXPIRED for key: {}", key);
            return null;
        }
        log.debug("In-memory cache HIT for key: {}", key);
        return entry.value();
    }

    private void localInvalidate(String key) {
        localCache.remove(key);
        log.debug("Invalidated in-memory cache for key: {}", key);
    }

    private boolean localHasKey(String key) {
        LocalCacheEntry entry = localCache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            localCache.remove(key, entry);
            return false;
        }
        return true;
    }

    private record LocalCacheEntry(Object value, long expiresAtMillis) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }
}
