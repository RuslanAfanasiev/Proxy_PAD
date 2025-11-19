package com.example.movie_api.repository;

import org.example.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IMovieRepository extends JpaRepository<Movie, Long> {

}
