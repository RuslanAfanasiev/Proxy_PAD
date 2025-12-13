package com.example.proxy.controller;

import com.example.proxy.interfaces.ICacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class SyncListenerController {

    private final ICacheService<String, Object> cacheService;

    @PostMapping("/invalidate/{id}")
    public ResponseEntity<Map<String, Object>> invalidateMovie(@PathVariable Long id) {
        return evictKeys(id);
    }

    // Legacy endpoint used by sync_node
    @PostMapping("/cache/invalidate/movies/{id}")
    public ResponseEntity<Map<String, Object>> invalidateMovieLegacy(@PathVariable Long id) {
        return evictKeys(id);
    }

    private ResponseEntity<Map<String, Object>> evictKeys(Long id) {
        cacheService.evict("/movies/" + id);
        cacheService.evict("/api/movies/" + id);
        return ResponseEntity.ok(Map.of(
                "message", "Cache invalidation processed",
                "id", id,
                "evictedKeys", new String[]{"/movies/" + id, "/api/movies/" + id}
        ));
    }
}
