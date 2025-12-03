package com.example.proxy.service;

import com.example.proxy.interfaces.ILoadBalancer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple round-robin load balancer cycling through a fixed list of backend URLs.
 */
public class RoundRobinLoadBalancerService implements ILoadBalancer {

    private final List<String> backends;
    private final AtomicInteger index = new AtomicInteger(0);

    public RoundRobinLoadBalancerService() {
        this(List.of());
    }

    public RoundRobinLoadBalancerService(List<String> backends) {
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
