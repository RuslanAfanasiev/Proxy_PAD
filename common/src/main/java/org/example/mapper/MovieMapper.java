package org.example.mapper;

import org.example.dto.MovieDTO;
import org.example.entities.Movie;

public class MovieMapper {

    public static MovieDTO movieDTO(Movie movie) {
        return new MovieDTO(
                movie.getId(),
                movie.getTitle(),
                movie.getRating(),
                movie.getLastChangedAt()
        );
    }

    public static Movie movieToEntity(MovieDTO dto) {
        Movie movie = new Movie();
        movie.setTitle(dto.getTitle());
        movie.setRating(dto.getRating());
        return movie;
    }
}
