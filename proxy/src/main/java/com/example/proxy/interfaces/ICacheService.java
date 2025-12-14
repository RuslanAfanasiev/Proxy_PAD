package com.example.proxy.interfaces;

/**
 * Generic cache contract with basic get/put semantics.
 */
public interface ICacheService<K, V> {

    V get(K key);

    void put(K key, V value);

    void evict(K key);
}
