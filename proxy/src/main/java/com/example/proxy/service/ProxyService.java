package com.example.proxy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * ProxyService handles smart proxying with caching and load balancing.
 * Acts as intermediary between clients and Data Warehouse nodes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProxyService {

    private final RestTemplate restTemplate;
    private final LoadBalancer loadBalancer;
    private final CacheService cacheService;

    /**
     * Forward GET request to Data Warehouse with caching.
     * GET requests are cached to reduce load on DW nodes.
     *
     * @param path Request path (e.g., /api/movies or /api/movies/1)
     * @return ResponseEntity from DW
     */
    public ResponseEntity<String> get(String path) {
        String cacheKey = cacheService.generateKey("GET", path);

        // Check cache first
        Object cachedResponse = cacheService.get(cacheKey);
        if (cachedResponse != null) {
            log.info("Cache HIT for GET {}", path);
            return ResponseEntity.ok()
                    .header("X-Cache", "HIT")
                    .body(cachedResponse.toString());
        }

        // Cache miss - forward to DW
        String dwNode = loadBalancer.getNextNode();
        String url = dwNode + path;

        log.info("Forwarding GET request to: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            // Cache successful responses
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                cacheService.put(cacheKey, response.getBody());
            }

            return ResponseEntity.status(response.getStatusCode())
                    .header("X-Cache", "MISS")
                    .header("X-DW-Node", dwNode)
                    .body(response.getBody());

        } catch (Exception e) {
            log.error("Error forwarding GET request to {}: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * Forward POST request to Data Warehouse.
     * POST requests invalidate cache and are not cached.
     *
     * @param path Request path
     * @param body Request body
     * @return ResponseEntity from DW
     */
    public ResponseEntity<String> post(String path, String body) {
        String dwNode = loadBalancer.getNextNode();
        String url = dwNode + path;

        log.info("Forwarding POST request to: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            // Invalidate cache for this path (list endpoint)
            String listCacheKey = cacheService.generateKey("GET", path);
            cacheService.invalidate(listCacheKey);

            return ResponseEntity.status(response.getStatusCode())
                    .header("X-DW-Node", dwNode)
                    .body(response.getBody());

        } catch (Exception e) {
            log.error("Error forwarding POST request to {}: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * Forward PUT request to Data Warehouse.
     * PUT requests invalidate related cache entries.
     *
     * @param path Request path (e.g., /api/movies/1)
     * @param body Request body
     * @return ResponseEntity from DW
     */
    public ResponseEntity<String> put(String path, String body) {
        String dwNode = loadBalancer.getNextNode();
        String url = dwNode + path;

        log.info("Forwarding PUT request to: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            // Invalidate cache for this specific resource
            String resourceCacheKey = cacheService.generateKey("GET", path);
            cacheService.invalidate(resourceCacheKey);

            // Also invalidate the list cache
            String basePath = path.substring(0, path.lastIndexOf('/'));
            String listCacheKey = cacheService.generateKey("GET", basePath);
            cacheService.invalidate(listCacheKey);

            return ResponseEntity.status(response.getStatusCode())
                    .header("X-DW-Node", dwNode)
                    .body(response.getBody());

        } catch (Exception e) {
            log.error("Error forwarding PUT request to {}: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * Forward DELETE request to Data Warehouse.
     * DELETE requests invalidate related cache entries.
     *
     * @param path Request path (e.g., /api/movies/1)
     * @return ResponseEntity from DW
     */
    public ResponseEntity<String> delete(String path) {
        String dwNode = loadBalancer.getNextNode();
        String url = dwNode + path;

        log.info("Forwarding DELETE request to: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    String.class
            );

            // Invalidate cache for this specific resource
            String resourceCacheKey = cacheService.generateKey("GET", path);
            cacheService.invalidate(resourceCacheKey);

            // Also invalidate the list cache
            String basePath = path.substring(0, path.lastIndexOf('/'));
            String listCacheKey = cacheService.generateKey("GET", basePath);
            cacheService.invalidate(listCacheKey);

            return ResponseEntity.status(response.getStatusCode())
                    .header("X-DW-Node", dwNode)
                    .body(response.getBody());

        } catch (Exception e) {
            log.error("Error forwarding DELETE request to {}: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Error: " + e.getMessage());
        }
    }
}
