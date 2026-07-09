package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesStore {

    private int nextId = 1;
    private final List<Movie> movies = new ArrayList<>();

    public Optional<Movie> findById(int id) {
        return movies.stream()
                .filter(movie -> movie.getId() == id)
                .findFirst();
    }

    public List<Movie> findByYear(int year) {
        return movies.stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public boolean removeById(int id) {
        return movies.removeIf(movie -> movie.getId() == id);
    }

    public Movie add(Movie movie) {
        Movie savedMovie = new Movie(
                nextId++,
                movie.getTitle(),
                movie.getYear()
        );

        movies.add(savedMovie);
        return savedMovie;
    }

    public List<Movie> getAll() {
        return List.copyOf(movies);
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}
