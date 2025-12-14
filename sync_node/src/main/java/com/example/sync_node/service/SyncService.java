package com.example.sync_node.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Service
@Slf4j
public class SyncService {

    private final RestTemplate restTemplate;
    private final String proxyUrl;

    public SyncService(RestTemplateBuilder restTemplateBuilder,
                       @Value("${proxy.url:http://localhost:8081}") String proxyUrl,
                       @Value("${sync.proxy.connect-timeout-ms:2000}") long connectTimeoutMs,
                       @Value("${sync.proxy.read-timeout-ms:2000}") long readTimeoutMs) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
        this.proxyUrl = proxyUrl;
    }

    //Notify the proxy to invalidate cached content for a specific movie.
    public ResponseEntity<String> invalidateMovieCache(Long movieId) {
        String target = UriComponentsBuilder.fromHttpUrl(proxyUrl)
                .pathSegment("proxy", "invalidate", String.valueOf(movieId))
                .toUriString();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(target, null, String.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception ex) {
            log.warn("Failed to notify proxy for movie cache invalidation (movieId={}): {}", movieId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body("Proxy invalidate request failed: " + ex.getMessage());
        }
    }
}
