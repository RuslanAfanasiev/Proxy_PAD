package com.example.proxy.service;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe in-memory cache with TTL-based expiration and background cleanup.
 */
@Service
public class InMemoryCacheService<K, V> implements CacheService<K, V> {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(1);
    private static final Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofSeconds(30);

    private final Duration ttl;
    private final ConcurrentMap<K, CacheEntry<V>> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;

    public InMemoryCacheService() {
        this(DEFAULT_TTL, DEFAULT_CLEANUP_INTERVAL);
    }

    public InMemoryCacheService(Duration ttl, Duration cleanupInterval) {
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        Objects.requireNonNull(cleanupInterval, "cleanupInterval");
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("in-memory-cache-cleaner");
            return t;
        });
        this.cleaner.scheduleAtFixedRate(this::evictExpired,
                cleanupInterval.toMillis(),
                cleanupInterval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public V get(K key) {
        CacheEntry<V> entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            store.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    @Override
    public void put(K key, V value) {
        long expiresAt = System.nanoTime() + ttl.toNanos();
        store.put(key, new CacheEntry<>(value, expiresAt));
    }

    private void evictExpired() {
        long now = System.nanoTime();
        store.forEach((key, entry) -> {
            if (entry.expiresAt() <= now) {
                store.remove(key, entry);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        cleaner.shutdownNow();
    }

    private record CacheEntry<V>(V value, long expiresAt) {
        boolean isExpired() {
            return expiresAt <= System.nanoTime();
        }
    }
}
