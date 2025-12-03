package com.example.sync_node.controller;

import com.example.sync_node.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/invalidate/{id}")
    public void invalidateMovie(@PathVariable Long id) {
        syncService.invalidateMovieCache(id);
    }
}
