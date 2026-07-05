package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;
    private final Gson gson;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
        this.gson = new Gson();
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

            store.add(movie);
            sendJson(ex, 201, gson.toJson(movie));
        } catch (JsonParseException e) {
            ErrorResponse error = new ErrorResponse("Некорректный JSON");
            sendJson(ex, 400, gson.toJson(error));
        }
    }
}
