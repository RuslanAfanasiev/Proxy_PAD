package com.example.movie_api.service;

import com.example.movie_api.repository.IMovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.example.entities.Movie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MovieService {

    private final IMovieRepository movieRepository;
    private final RestTemplate restTemplate;
    private final String syncNodeUrl;

    public MovieService(IMovieRepository movieRepository,
                        RestTemplateBuilder restTemplateBuilder,
                        @Value("${sync.node.url:http://localhost:9002}") String syncNodeUrl) {
        this.movieRepository = movieRepository;
        this.restTemplate = restTemplateBuilder.build();
        this.syncNodeUrl = syncNodeUrl;
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    public Movie createMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    public Optional<Movie> updateMovie(Long id, Movie updatedMovie) {
        return movieRepository.findById(id)
                .map(existing -> {
                    existing.setTitle(updatedMovie.getTitle());
                    existing.setRating(updatedMovie.getRating());
                    return updateMovie(existing);
                });
    }

    public Movie updateMovie(Movie movie) {
        Movie saved = movieRepository.save(movie);
        notifySyncNode(saved);
        return saved;
    }

    public boolean deleteMovie(Long id) {
        return movieRepository.findById(id)
                .map(existing -> {
                    movieRepository.delete(existing);
                    return true;
                })
                .orElse(false);
    }

    private void notifySyncNode(Movie movie) {
        String url = syncNodeUrl + "/sync/movies";
        try {
            restTemplate.postForLocation(url, movie);
        } catch (Exception ex) {
            log.warn("Failed to notify Sync_Node at {} for movie {}: {}", url, movie.getId(), ex.getMessage());
        }
    }
}
