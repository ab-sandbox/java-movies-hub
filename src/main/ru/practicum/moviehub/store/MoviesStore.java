package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class MoviesStore {

    private final List<Movie> movies = new ArrayList<>();

    public void add(Movie movie) {
        movies.add(movie);
    }

    public List<Movie> getAll() {
        return List.copyOf(movies);
    }

    public void clear() {
        movies.clear();
    }
}
