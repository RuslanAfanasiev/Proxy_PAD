package com.example.proxy.service;

import com.example.proxy.http.HttpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Coordinates cache lookup, load balancer selection, and HTTP forwarding.
 */
@Service
@RequiredArgsConstructor
public class ProxyService {

    private final CacheService<String, String> cacheService;
    private final LoadBalancer loadBalancer;
    private final HttpClient httpClient;

    public String forwardGet(String pathAndQuery) {
        String cacheKey = buildCacheKey(pathAndQuery);

        String cached = cacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String backend = loadBalancer.getNextUrl();
        String response = httpClient.forwardGet(backend, pathAndQuery);

        if (response != null) {
            cacheService.put(cacheKey, response);
        }

        return response;
    }

    public String forwardPost(String pathAndQuery, Object body) {
        String backend = loadBalancer.getNextUrl();
        return httpClient.forwardPost(backend, pathAndQuery, body);
    }

    private String buildCacheKey(String pathAndQuery) {
        return pathAndQuery;
    }
}
