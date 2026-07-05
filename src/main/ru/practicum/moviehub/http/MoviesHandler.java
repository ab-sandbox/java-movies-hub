package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class MoviesHandler extends BaseHttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            sendJson(exchange, 200, "[]");
        }
    }
}
