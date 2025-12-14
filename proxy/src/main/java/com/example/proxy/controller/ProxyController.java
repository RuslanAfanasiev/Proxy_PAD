package com.example.proxy.controller;

import com.example.proxy.service.ProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ProxyController handles all HTTP requests and forwards them to Data Warehouse nodes.
 * Supports both JSON and XML response formats based on Accept header.
 */
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class ProxyController {

    private final ProxyService proxyService;

    /**
     * Get all movies.
     * Supports JSON and XML response formats.
     *
     * @return List of movies
     */
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<String> getAllMovies() {
        log.info("GET /api/movies - Get all movies");
        return proxyService.get("/api/movies");
    }

    /**
     * Get movie by ID.
     * Supports JSON and XML response formats.
     *
     * @param id Movie ID
     * @return Movie details
     */
    @GetMapping(value = "/{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<String> getMovieById(@PathVariable Long id) {
        log.info("GET /api/movies/{} - Get movie by ID", id);
        return proxyService.get("/api/movies/" + id);
    }

    /**
     * Create new movie.
     * Accepts JSON and XML request formats.
     *
     * @param body Movie data
     * @return Created movie
     */
    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    public ResponseEntity<String> createMovie(@RequestBody String body) {
        log.info("POST /api/movies - Create new movie");
        return proxyService.post("/api/movies", body);
    }

    /**
     * Update existing movie.
     * Accepts JSON and XML request formats.
     *
     * @param id Movie ID
     * @param body Updated movie data
     * @return Updated movie
     */
    @PutMapping(
            value = "/{id}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    public ResponseEntity<String> updateMovie(@PathVariable Long id, @RequestBody String body) {
        log.info("PUT /api/movies/{} - Update movie", id);
        return proxyService.put("/api/movies/" + id, body);
    }

    /**
     * Delete movie by ID.
     *
     * @param id Movie ID
     * @return No content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMovie(@PathVariable Long id) {
        log.info("DELETE /api/movies/{} - Delete movie", id);
        return proxyService.delete("/api/movies/" + id);
    }

    /**
     * Health check endpoint to verify proxy is running.
     *
     * @return Status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok()
                .header("X-Proxy-App", "smart-proxy")
                .body("{\"status\":\"UP\",\"service\":\"Smart Proxy\"}");
    }
}
