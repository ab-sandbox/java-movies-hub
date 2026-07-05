package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";

    private static MoviesStore store;

    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();

        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies должен вернуть 200"
        );

        String contentTypeHeaderValue = resp.headers()
                .firstValue("Content-Type")
                .orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );

        String body = resp.body().trim();

        assertTrue(
                body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив"
        );
    }

    @Test
    void getMovies_whenStoreHasMovie_returnsMovie() throws Exception {
        store.add(new Movie(1, "Interstellar", 2014));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(200, resp.statusCode());

        assertTrue(
                resp.body().contains("Interstellar"),
                "Ответ должен содержать фильм из хранилища"
        );
    }

    @Test
    void postMovie_whenValid_addsMovie() throws Exception {
        String json = """
                {
                  "id": 1,
                  "title": "Interstellar",
                  "year": 2014
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.ofString(
                        json,
                        StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                201,
                resp.statusCode(),
                "POST /movies должен вернуть 201"
        );

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        HttpResponse<String> getResp = send(getReq);

        assertTrue(
                getResp.body().contains("Interstellar"),
                "Добавленный фильм должен возвращаться в GET /movies"
        );
    }

    @Test
    void postMovie_whenJsonInvalid_returnsBadRequest() throws Exception {
        String invalidJson = """
                {
                  "id": 1,
                  "title": "Interstellar",
                  "year":
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.ofString(
                        invalidJson,
                        StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                400,
                resp.statusCode(),
                "POST /movies с некорректным JSON должен вернуть 400"
        );

        assertEquals(
                "application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""),
                "Ошибка должна возвращаться в формате JSON"
        );

        assertTrue(
                resp.body().contains("error"),
                "Ответ должен содержать описание ошибки"
        );
    }

    @Test
    void postMovie_whenTitleEmpty_returnsBadRequest() throws Exception {
        String json = """
                {
                  "id": 1,
                  "title": "",
                  "year": 2014
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.ofString(
                        json,
                        StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                400,
                resp.statusCode(),
                "POST /movies с пустым названием должен вернуть 400"
        );

        assertTrue(
                resp.body().contains("error"),
                "Ответ должен содержать описание ошибки"
        );
    }

    @Test
    void postMovie_whenYearInvalid_returnsBadRequest() throws Exception {
        String json = """
                {
                  "id": 1,
                  "title": "Interstellar",
                  "year": 0
                }
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.ofString(
                        json,
                        StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                400,
                resp.statusCode(),
                "POST /movies с некорректным годом должен вернуть 400"
        );

        assertTrue(
                resp.body().contains("error"),
                "Ответ должен содержать описание ошибки"
        );
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        store.add(new Movie(1, "Interstellar", 2014));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies/{id} должен вернуть 200 для существующего фильма"
        );

        String body = resp.body().trim();

        assertTrue(
                body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON-объект фильма"
        );

        assertTrue(
                body.contains("Interstellar"),
                "Ответ должен содержать найденный фильм"
        );
    }

    @Test
    void getMovieById_whenNotExists_returnsNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                404,
                resp.statusCode(),
                "GET /movies/{id} должен вернуть 404 для несуществующего фильма"
        );

        assertTrue(
                resp.body().contains("error"),
                "Ответ должен содержать описание ошибки"
        );
    }

    @Test
    void deleteMovie_whenExists_removesMovie() throws Exception {
        store.add(new Movie(1, "Interstellar", 2014));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .timeout(Duration.ofSeconds(2))
                .DELETE()
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                204,
                resp.statusCode(),
                "DELETE /movies/{id} должен вернуть 204"
        );

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        HttpResponse<String> getResp = send(getReq);

        assertEquals(
                404,
                getResp.statusCode(),
                "Удаленный фильм не должен находиться"
        );
    }

    @Test
    void deleteMovie_whenNotExists_returnsNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .timeout(Duration.ofSeconds(2))
                .DELETE()
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                404,
                resp.statusCode(),
                "DELETE /movies/{id} должен вернуть 404 для несуществующего фильма"
        );

        assertTrue(
                resp.body().contains("error"),
                "Ответ должен содержать описание ошибки"
        );
    }

    @Test
    void putMovies_whenMethodUnsupported_returnsMethodNotAllowed() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .timeout(Duration.ofSeconds(2))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                405,
                resp.statusCode(),
                "Неподдерживаемый HTTP-метод должен вернуть 405"
        );

        assertTrue(
                resp.body().contains("error"),
                "Ответ должен содержать описание ошибки"
        );
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }
}
