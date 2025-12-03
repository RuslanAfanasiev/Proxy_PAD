package com.example.proxy.controller;

import com.example.proxy.interfaces.ICacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class SyncListenerController {

    private final ICacheService<String, String> ICacheService;

    @PostMapping("/invalidate/{id}")
    public void invalidateMovie(@PathVariable Long id) {
        evictKeys(id);
    }

    // Legacy endpoint used by sync_node
    @PostMapping("/cache/invalidate/movies/{id}")
    public void invalidateMovieLegacy(@PathVariable Long id) {
        evictKeys(id);
    }

    private void evictKeys(Long id) {
        ICacheService.evict("/movies/" + id);
        ICacheService.evict("/api/movies/" + id);
    }
}
