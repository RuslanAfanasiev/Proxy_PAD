package com.example.proxy.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple round-robin load balancer cycling through a fixed list of backend URLs.
 */
@Service
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final List<String> backends;
    private final AtomicInteger index = new AtomicInteger(0);

    public RoundRobinLoadBalancer() {
        this(List.of());
    }

    public RoundRobinLoadBalancer(List<String> backends) {
        this.backends = List.copyOf(Objects.requireNonNull(backends, "backends"));
    }

    @Override
    public String getNextUrl() {
        if (backends.isEmpty()) {
            throw new IllegalStateException("No backend URLs configured for load balancer");
        }
        int position = Math.floorMod(index.getAndIncrement(), backends.size());
        return backends.get(position);
    }
}
