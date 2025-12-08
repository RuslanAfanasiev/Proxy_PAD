package com.example.proxy.http;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

// Lightweight HTTP forwarder built on RestTemplate.
@Component
public class HttpClient {

    private final RestTemplate restTemplate = new RestTemplate();

    // Forward a GET request to the target backend and return the raw body as a string.
    public String forwardGet(String baseUrl, String pathAndQuery) {
        String target = buildUrl(baseUrl, pathAndQuery);
        return restTemplate.getForObject(target, String.class);
    }

    // Forward a POST request with a JSON payload to the target backend and return the raw body.
    public String forwardPost(String baseUrl, String pathAndQuery, Object body) {
        String target = buildUrl(baseUrl, pathAndQuery);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForObject(target, new HttpEntity<>(body, headers), String.class);
    }

    public String forwardPut(String baseUrl, String pathAndQuery, Object body) {
        String target = buildUrl(baseUrl, pathAndQuery);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(target, HttpMethod.PUT,
                new HttpEntity<>(body, headers), String.class).getBody();
    }

    public String forwardDelete(String baseUrl, String pathAndQuery) {
        String target = buildUrl(baseUrl, pathAndQuery);
        return restTemplate.exchange(target, HttpMethod.DELETE,
                null, String.class).getBody();
    }

    private String buildUrl(String baseUrl, String pathAndQuery) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(pathAndQuery)
                .build(true)
                .toUriString();
    }
}
