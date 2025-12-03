package com.example.proxy.service;

/**
 * Generic cache contract with basic get/put semantics.
 */
public interface CacheService<K, V> {

    V get(K key);

    void put(K key, V value);
}
