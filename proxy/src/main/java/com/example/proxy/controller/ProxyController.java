package com.example.proxy.controller;

import com.example.proxy.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/**")
@RequiredArgsConstructor
@Slf4j
public class ProxyController {

    private final ProxyService proxyService;

    /**
     * Handle all GET requests and forward them to the data warehouse via proxy.
     */
    @GetMapping
    public ResponseEntity<String> handleGet(HttpServletRequest request) {
        String path = extractPath(request);
        log.info("Proxy GET request: {}", path);
        return proxyService.handleGet(path);
    }

    /**
     * Handle all POST requests and forward them to the data warehouse via proxy.
     */
    @PostMapping
    public ResponseEntity<String> handlePost(HttpServletRequest request, @RequestBody String body) {
        String path = extractPath(request);
        log.info("Proxy POST request: {}", path);
        return proxyService.handlePost(path, body);
    }

    /**
     * Handle all PUT requests and forward them to the data warehouse via proxy.
     */
    @PutMapping
    public ResponseEntity<String> handlePut(HttpServletRequest request, @RequestBody String body) {
        String path = extractPath(request);
        log.info("Proxy PUT request: {}", path);
        return proxyService.handlePut(path, body);
    }

    /**
     * Handle all DELETE requests and forward them to the data warehouse via proxy.
     */
    @DeleteMapping
    public ResponseEntity<String> handleDelete(HttpServletRequest request) {
        String path = extractPath(request);
        log.info("Proxy DELETE request: {}", path);
        return proxyService.handleDelete(path);
    }

    /**
     * Extract the full path from the HTTP request.
     */
    private String extractPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        if (queryString != null && !queryString.isEmpty()) {
            return uri + "?" + queryString;
        }
        return uri;
    }
}
