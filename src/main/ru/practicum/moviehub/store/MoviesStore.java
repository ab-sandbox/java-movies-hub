package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesStore {

    private final List<Movie> movies = new ArrayList<>();

    public Optional<Movie> findById(int id) {
        return movies.stream()
                .filter(movie -> movie.getId() == id)
                .findFirst();
    }

    public boolean removeById(int id) {
        return movies.removeIf(movie -> movie.getId() == id);
    }

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
