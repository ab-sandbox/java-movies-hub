package ru.practicum.moviehub.model;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class Movie {

    private static final int MIN_RELEASE_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;

    private final int id;
    private final String title;
    private final int year;

    public Movie(int id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public Movie(String title, int year) {
        this(0, title, year);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();

        if (title == null || title.isBlank()) {
            errors.add("Название не должно быть пустым");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            errors.add("Название не должно быть длиннее 100 символов");
        }

        if (year < MIN_RELEASE_YEAR) {
            errors.add("Год выпуска должен быть не раньше 1888 года");
        } else if (year > Year.now().getValue() + 1) {
            errors.add("Год выпуска не должен быть позже следующего года");
        }

        return errors;
    }
}
