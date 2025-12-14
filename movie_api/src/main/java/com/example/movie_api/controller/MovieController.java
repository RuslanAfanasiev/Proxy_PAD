package com.example.movie_api.controller;

import com.example.movie_api.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.example.dto.MovieDTO;
import org.example.entities.Movie;
import org.example.mapper.MovieMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/movies", "/api/movies"})
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public List<MovieDTO> getAllMovies() {
        return movieService.getAllMovies()
                .stream()
                .map(MovieMapper::movieDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieDTO> getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .map(MovieMapper::movieDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public MovieDTO createMovie(@RequestBody MovieDTO movieDto) {
        Movie saved = movieService.createMovie(MovieMapper.movieToEntity(movieDto));
        return MovieMapper.movieDTO(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieDTO> updateMovie(
            @PathVariable Long id,
            @RequestBody MovieDTO updatedMovie) {
        return movieService.updateMovie(id, MovieMapper.movieToEntity(updatedMovie))
                .map(MovieMapper::movieDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public ResponseEntity<?> deleteMovieByParam(@RequestParam("id") Long id) {
        return deleteMovieInternal(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovie(@PathVariable Long id) {
        return deleteMovieInternal(id);
    }

    private ResponseEntity<?> deleteMovieInternal(Long id) {
        boolean deleted = movieService.deleteMovie(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
