package com.example.proxy.service;

import com.example.proxy.http.HttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyService {

    private final HttpClient httpClient;
    private final LoadBalancer loadBalancer;
    private final CacheService cacheService;

    /**
     * Handle GET requests with caching support.
     * Checks cache first, if miss - forwards to DW using load balancer.
     */
    public ResponseEntity<String> handleGet(String path) {
        String cacheKey = cacheService.generateKey("GET", path);

        // Try to get from cache
        String cachedResponse = cacheService.get(cacheKey);
        if (cachedResponse != null) {
            return ResponseEntity.ok(cachedResponse);
        }

        // Cache miss - forward to data warehouse
        String endpoint = loadBalancer.getNextEndpoint();
        String url = endpoint + path;
        ResponseEntity<String> response = httpClient.get(url);

        // Cache successful responses
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            cacheService.put(cacheKey, response.getBody());
        }

        return response;
    }

    /**
     * Handle POST requests.
     * Invalidates cache and forwards to DW using load balancer.
     */
    public ResponseEntity<String> handlePost(String path, String body) {
        String endpoint = loadBalancer.getNextEndpoint();
        String url = endpoint + path;

        ResponseEntity<String> response = httpClient.post(url, body);

        // Invalidate related cache entries
        if (response.getStatusCode().is2xxSuccessful()) {
            cacheService.invalidatePattern("GET:" + extractBasePath(path) + "*");
        }

        return response;
    }

    /**
     * Handle PUT requests.
     * Invalidates cache and forwards to DW using load balancer.
     */
    public ResponseEntity<String> handlePut(String path, String body) {
        String endpoint = loadBalancer.getNextEndpoint();
        String url = endpoint + path;

        ResponseEntity<String> response = httpClient.put(url, body);

        // Invalidate related cache entries
        if (response.getStatusCode().is2xxSuccessful()) {
            cacheService.invalidatePattern("GET:" + extractBasePath(path) + "*");
        }

        return response;
    }

    /**
     * Handle DELETE requests.
     * Invalidates cache and forwards to DW using load balancer.
     */
    public ResponseEntity<String> handleDelete(String path) {
        String endpoint = loadBalancer.getNextEndpoint();
        String url = endpoint + path;

        ResponseEntity<String> response = httpClient.delete(url);

        // Invalidate related cache entries
        if (response.getStatusCode().is2xxSuccessful()) {
            cacheService.invalidatePattern("GET:" + extractBasePath(path) + "*");
        }

        return response;
    }

    /**
     * Extract base path from a URL path (e.g., "/api/movies/1" -> "/api/movies").
     */
    private String extractBasePath(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            return path.substring(0, lastSlash);
        }
        return path;
    }
}
