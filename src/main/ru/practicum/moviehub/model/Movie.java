package ru.practicum.moviehub.model;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class Movie {

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
        } else if (title.length() > 100) {
            errors.add("Название не должно быть длиннее 100 символов");
        }

        if (year < 1888) {
            errors.add("Год выпуска должен быть не раньше 1888 года");
        } else if (year > Year.now().getValue() + 1) {
            errors.add("Год выпуска не должен быть позже следующего года");
        }

        return errors;
    }
}
