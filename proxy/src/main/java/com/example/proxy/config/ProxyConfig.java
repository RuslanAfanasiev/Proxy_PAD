package com.example.proxy.config;

import com.example.proxy.interfaces.ICacheService;
import com.example.proxy.interfaces.ILoadBalancer;
import com.example.proxy.service.InMemoryCacheService;
import com.example.proxy.service.RoundRobinLoadBalancerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class ProxyConfig {

    @Value("${proxy.backends}")
    private String backendUrls;

    @Bean
    public ILoadBalancer loadBalancer() {
        List<String> backends = Arrays.stream(backendUrls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (backends.isEmpty()) {
            throw new IllegalStateException("proxy.backends must contain at least one URL");
        }
        return new RoundRobinLoadBalancerService(backends);
    }

    @Bean
    public ICacheService<String, String> cacheService() {
        return new InMemoryCacheService<>();
    }
}
