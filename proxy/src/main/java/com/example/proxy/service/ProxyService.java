package com.example.proxy.service;

import com.example.proxy.http.HttpClient;
import com.example.proxy.interfaces.ICacheService;
import com.example.proxy.interfaces.ILoadBalancer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Coordinates cache lookup, load balancer selection, and HTTP forwarding.
 */
@Service
@RequiredArgsConstructor
public class ProxyService {

    private final ICacheService<String, String> ICacheService;
    private final ILoadBalancer loadBalancer;
    private final HttpClient httpClient;

    public String forwardGet(String pathAndQuery) {
        String cacheKey = buildCacheKey(pathAndQuery);

        String cached = ICacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String backend = loadBalancer.getNextUrl();
        String response = httpClient.forwardGet(backend, pathAndQuery);

        if (response != null) {
            ICacheService.put(cacheKey, response);
        }

        return response;
    }

    public String forwardPost(String pathAndQuery, Object body) {
        String backend = loadBalancer.getNextUrl();
        return httpClient.forwardPost(backend, pathAndQuery, body);
    }


    public String forwardPut(String pathAndQuery, Object body) {
        String backend = loadBalancer.getNextUrl();
        return httpClient.forwardPut(backend, pathAndQuery, body);
    }

    public String forwardDelete(String pathAndQuery) {
        String backend = loadBalancer.getNextUrl();
        return httpClient.forwardDelete(backend, pathAndQuery);
    }

    private String buildCacheKey(String pathAndQuery) {
        return pathAndQuery;
    }
}
