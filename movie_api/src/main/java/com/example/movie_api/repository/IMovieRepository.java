package com.example.movie_api.repository;

import org.example.entities.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IMovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByLastChangedAtAfter(LocalDateTime timestamp);
}
