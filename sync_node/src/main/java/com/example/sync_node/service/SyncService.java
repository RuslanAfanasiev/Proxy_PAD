package com.example.sync_node.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class SyncService {

    private final RestTemplate restTemplate;
    private final String proxyUrl;

    public SyncService(RestTemplateBuilder restTemplateBuilder,
                       @Value("${proxy.url:http://localhost:8081}") String proxyUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.proxyUrl = proxyUrl;
    }

    public void invalidateMovieCache(Long movieId) {
        String target = UriComponentsBuilder.fromHttpUrl(proxyUrl)
                .path("/proxy/cache/invalidate/movies/")
                .path(String.valueOf(movieId))
                .toUriString();
        try {
            restTemplate.postForLocation(target, null);
        } catch (Exception ex) {
            log.warn("Failed to notify proxy for movie {}: {}", movieId, ex.getMessage());
        }
    }
}
