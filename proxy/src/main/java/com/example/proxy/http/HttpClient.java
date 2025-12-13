package com.example.proxy.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class HttpClient {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Execute a GET request to the specified endpoint.
     */
    public ResponseEntity<String> get(String url) {
        try {
            log.info("GET request to: {}", url);
            return restTemplate.getForEntity(url, String.class);
        } catch (HttpClientErrorException e) {
            log.error("GET request failed: {} - {}", e.getStatusCode(), e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("GET request error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Execute a POST request to the specified endpoint with a JSON body.
     */
    public ResponseEntity<String> post(String url, String jsonBody) {
        try {
            log.info("POST request to: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            return restTemplate.postForEntity(url, request, String.class);
        } catch (HttpClientErrorException e) {
            log.error("POST request failed: {} - {}", e.getStatusCode(), e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("POST request error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Execute a PUT request to the specified endpoint with a JSON body.
     */
    public ResponseEntity<String> put(String url, String jsonBody) {
        try {
            log.info("PUT request to: {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            restTemplate.put(url, request);
            return ResponseEntity.ok(jsonBody);
        } catch (HttpClientErrorException e) {
            log.error("PUT request failed: {} - {}", e.getStatusCode(), e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("PUT request error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Execute a DELETE request to the specified endpoint.
     */
    public ResponseEntity<String> delete(String url) {
        try {
            log.info("DELETE request to: {}", url);
            restTemplate.delete(url);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            log.error("DELETE request failed: {} - {}", e.getStatusCode(), e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("DELETE request error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
