package ru.practicum.moviehub.http;

import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        try {
            if (method.equalsIgnoreCase("GET")) {
                handleGet(ex);
            } else if (method.equalsIgnoreCase("POST")) {
                handlePost(ex);
            } else if (method.equalsIgnoreCase("DELETE")) {
                handleDelete(ex);
            } else {
                sendError(ex, 405, "Метод не поддерживается");
            }
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Некорректный идентификатор фильма");
        }
    }

    private void handleGet(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        if (path.equals("/movies")) {
            handleGetMovies(ex);
            return;
        }

        int id = extractMovieId(ex);
        Optional<Movie> movie = store.findById(id);

        if (movie.isEmpty()) {
            sendError(ex, 404, "Фильм не найден");
            return;
        }

        sendJson(ex, 200, gson.toJson(movie.get()));
    }

    private void handleGetMovies(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();

        if (query == null) {
            sendJson(ex, 200, gson.toJson(store.getAll()));
            return;
        }

        if (!query.startsWith("year=")) {
            sendError(
                    ex,
                    400,
                    "Некорректный параметр запроса - 'year'"
            );
            return;
        }

        try {
            int year = Integer.parseInt(
                    query.substring("year=".length())
            );
            sendJson(ex, 200, gson.toJson(store.findByYear(year)));
        } catch (NumberFormatException e) {
            sendError(
                    ex,
                    400,
                    "Некорректный параметр запроса - 'year'"
            );
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        if (!hasJsonContentType(ex)) {
            sendError(
                    ex,
                    415,
                    "Неподдерживаемый тип данных"
            );
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(
                ex.getRequestBody(),
                StandardCharsets.UTF_8
        )) {
            Movie movie = gson.fromJson(reader, Movie.class);

            List<String> validationErrors = movie.getValidationErrors();

            if (!validationErrors.isEmpty()) {
                sendJson(
                        ex,
                        422,
                        gson.toJson(
                                new ErrorResponse(
                                        "Ошибка валидации",
                                        validationErrors
                                )
                        )
                );
                return;
            }

            Movie savedMovie = store.add(movie);
            sendJson(ex, 201, gson.toJson(savedMovie));
        } catch (JsonParseException e) {
            sendError(ex, 400, "Некорректный JSON");
        }
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        int id = extractMovieId(ex);
        boolean removed = store.removeById(id);

        if (!removed) {
            sendError(ex, 404, "Фильм не найден");
            return;
        }

        sendNoContent(ex);
    }

    private int extractMovieId(HttpExchange ex) {
        String path = ex.getRequestURI().getPath();
        String idPart = path.substring("/movies/".length());

        return Integer.parseInt(idPart);
    }

    private boolean hasJsonContentType(HttpExchange ex) {
        String contentType = ex.getRequestHeaders()
                .getFirst("Content-Type");

        return contentType != null
               && contentType.toLowerCase(Locale.ROOT)
                       .startsWith("application/json");
    }
}
