package com.example.proxy.controller;

import com.example.proxy.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/proxy/cache")
@RequiredArgsConstructor
public class CacheDebugController {

    private final CacheService cacheService;

    @GetMapping("/has")
    public Map<String, Object> has(
            @RequestParam(required = false) String key,
            @RequestParam(required = false, defaultValue = "GET") String method,
            @RequestParam(required = false) String path
    ) {
        String resolvedKey = key;
        if (resolvedKey == null) {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("Provide either 'key' or 'path'.");
            }
            resolvedKey = cacheService.generateKey(method, path);
        }

        return Map.of(
                "key", resolvedKey,
                "exists", cacheService.hasKey(resolvedKey)
        );
    }
}

