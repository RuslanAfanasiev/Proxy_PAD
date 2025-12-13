package com.example.proxy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoadBalancer {

    @Value("${datawarehouse.endpoints}")
    private List<String> endpoints;

    private final AtomicInteger currentIndex = new AtomicInteger(0);

    /**
     * Get the next endpoint using Round-Robin algorithm.
     * Thread-safe implementation using AtomicInteger.
     */
    public String getNextEndpoint() {
        if (endpoints == null || endpoints.isEmpty()) {
            throw new IllegalStateException("No data warehouse endpoints configured");
        }

        int index = currentIndex.getAndUpdate(i -> (i + 1) % endpoints.size());
        return endpoints.get(index);
    }

    /**
     * Get all configured endpoints.
     */
    public List<String> getAllEndpoints() {
        return endpoints;
    }

    /**
     * Get the number of available endpoints.
     */
    public int getEndpointCount() {
        return endpoints != null ? endpoints.size() : 0;
    }
}
