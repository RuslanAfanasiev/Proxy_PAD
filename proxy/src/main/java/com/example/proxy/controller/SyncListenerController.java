package com.example.proxy.controller;

import com.example.proxy.interfaces.ICacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class SyncListenerController {

    private final ICacheService<String, String> cache;

    @PostMapping("/invalidate/{id}")
    public ResponseEntity<Map<String, Object>> invalidateMovie(@PathVariable Long id) {
        return evictKeys(id);
    }

    @PostMapping("/cache/invalidate/movies/{id}")
    public ResponseEntity<Map<String, Object>> invalidateMovieLegacy(@PathVariable Long id) {
        return evictKeys(id);
    }

    private ResponseEntity<Map<String, Object>> evictKeys(Long id) {
        cache.evict("/movies/" + id);
        cache.evict("/api/movies/" + id);

        // IMPORTANT!
        cache.evict("/movies");
        cache.evict("/api/movies");

        return ResponseEntity.ok(Map.of(
                "message", "Cache invalidation processed",
                "id", id,
                "evicted", new String[]{
                        "/movies/" + id,
                        "/api/movies/" + id,
                        "/movies",
                        "/api/movies"
                }
        ));
    }
}
