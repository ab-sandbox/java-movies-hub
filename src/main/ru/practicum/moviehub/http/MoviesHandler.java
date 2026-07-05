package ru.practicum.moviehub.http;

import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            sendJson(ex, 200, gson.toJson(store.getAll()));
        } else if (method.equalsIgnoreCase("POST")) {
            handlePost(ex);
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                ex.getRequestBody(),
                StandardCharsets.UTF_8
        )) {
            Movie movie = gson.fromJson(reader, Movie.class);

            if (!movie.isValid()) {
                sendError(
                        ex,
                        400,
                        "Название фильма не должно быть пустым"
                );
                return;
            }

            store.add(movie);
            sendJson(ex, 201, gson.toJson(movie));
        } catch (JsonParseException e) {
            sendError(ex, 400, "Некорректный JSON");
        }
    }
}
