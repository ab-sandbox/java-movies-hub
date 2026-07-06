package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";

    private static MoviesStore store;

    private static MoviesServer server;
    private static HttpClient client;

    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();

        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        gson = new Gson();
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

        List<Movie> movies = gson.fromJson(
                resp.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertTrue(
                movies.isEmpty(),
                "Ожидается пустой список фильмов"
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

        List<Movie> movies = gson.fromJson(
                resp.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(
                1,
                movies.size(),
                "Ответ должен содержать один фильм"
        );

        assertEquals(
                "Interstellar",
                movies.getFirst().getTitle(),
                "Ответ должен содержать фильм из хранилища"
        );
    }

    @Test
    void postMovie_whenValid_addsMovie() throws Exception {
        String json = "{\n" +
                      "  \"id\": 1,\n" +
                      "  \"title\": \"Interstellar\",\n" +
                      "  \"year\": 2014\n" +
                      "}\n";

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

        List<Movie> movies = gson.fromJson(
                getResp.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(
                1,
                movies.size(),
                "GET /movies должен вернуть добавленный фильм"
        );

        assertEquals(
                "Interstellar",
                movies.getFirst().getTitle(),
                "Добавленный фильм должен возвращаться в GET /movies"
        );
    }

    @Test
    void postMovie_whenJsonInvalid_returnsBadRequest() throws Exception {
        String invalidJson = "{\n" +
                             "  \"id\": 1,\n" +
                             "  \"title\": \"Interstellar\",\n" +
                             "  \"year\":\n" +
                             "}\n";

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
    void postMovie_whenTitleEmpty_returnsUnprocessableEntity() throws Exception {
        String json = "{\n" +
                      "  \"title\": \"\",\n" +
                      "  \"year\": 2014\n" +
                      "}\n";

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
                422,
                resp.statusCode(),
                "POST /movies с ошибками валидации должен вернуть 422"
        );

        ErrorResponse error = gson.fromJson(
                resp.body(),
                ErrorResponse.class
        );

        assertEquals(
                "Ошибка валидации",
                error.getError()
        );

        assertEquals(
                List.of("Название не должно быть пустым"),
                error.getDetails()
        );
    }

    @Test
    void postMovie_whenYearTooEarly_returnsUnprocessableEntity() throws Exception {
        String json = "{\n" +
                      "  \"title\": \"Interstellar\",\n" +
                      "  \"year\": 1887\n" +
                      "}\n";

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
                422,
                resp.statusCode(),
                "POST /movies с ошибками валидации должен вернуть 422"
        );

        ErrorResponse error = gson.fromJson(
                resp.body(),
                ErrorResponse.class
        );

        assertEquals(
                "Ошибка валидации",
                error.getError()
        );

        assertEquals(
                List.of("Год выпуска должен быть не раньше 1888 года"),
                error.getDetails()
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

        Movie movie = gson.fromJson(resp.body(), Movie.class);

        assertEquals(
                "Interstellar",
                movie.getTitle(),
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

    @Test
    void getMovieById_whenIdInvalid_returnsBadRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                400,
                resp.statusCode(),
                "GET /movies/{id} с некорректным id должен вернуть 400"
        );

        assertTrue(
                resp.body().contains("error"),
                "Ответ должен содержать описание ошибки"
        );
    }

    @Test
    void deleteMovie_whenIdInvalid_returnsBadRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .timeout(Duration.ofSeconds(2))
                .DELETE()
                .build();

        HttpResponse<String> resp = send(req);

        assertEquals(
                400,
                resp.statusCode(),
                "DELETE /movies/{id} с некорректным id должен вернуть 400"
        );

        assertTrue(
                resp.body().contains("error"),
                "Ответ должен содержать описание ошибки"
        );
    }

    @Test
    void postMovie_whenMultipleFieldsInvalid_returnsAllValidationErrors() throws Exception {
        String json = "{\n" +
                      "  \"title\": \"\",\n" +
                      "  \"year\": 0\n" +
                      "}\n";

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
                422,
                resp.statusCode(),
                "POST /movies с ошибками валидации должен вернуть 422"
        );

        ErrorResponse error = gson.fromJson(
                resp.body(),
                ErrorResponse.class
        );

        assertEquals(
                "Ошибка валидации",
                error.getError()
        );

        assertEquals(
                2,
                error.getDetails().size(),
                "Ответ должен содержать все ошибки валидации"
        );

        assertTrue(
                error.getDetails().contains("Название не должно быть пустым"),
                "Ответ должен содержать ошибку названия"
        );

        assertTrue(
                error.getDetails().contains("Год выпуска должен быть не раньше 1888 года"),
                "Ответ должен содержать ошибку года выпуска"
        );
    }

    @Test
    void postMovie_whenTitleTooLong_returnsUnprocessableEntity() throws Exception {
        String longTitle = "A".repeat(101);

        String json = "{\n" +
                      "  \"title\": \"" + longTitle + "\",\n" +
                      "  \"year\": 2014\n" +
                      "}\n";

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
                422,
                resp.statusCode(),
                "Название длиннее 100 символов должно вернуть 422"
        );

        ErrorResponse error = gson.fromJson(
                resp.body(),
                ErrorResponse.class
        );

        assertEquals(
                List.of("Название не должно быть длиннее 100 символов"),
                error.getDetails()
        );
    }

    @Test
    void postMovie_whenYearTooLate_returnsUnprocessableEntity() throws Exception {
        int invalidYear = Year.now().getValue() + 2;

        String json = "{\n" +
                      "  \"title\": \"Interstellar\",\n" +
                      "  \"year\": " + invalidYear + "\n" +
                      "}\n";

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
                422,
                resp.statusCode(),
                "Слишком поздний год выпуска должен вернуть 422"
        );

        ErrorResponse error = gson.fromJson(
                resp.body(),
                ErrorResponse.class
        );

        assertEquals(
                List.of("Год выпуска не должен быть позже следующего года"),
                error.getDetails()
        );
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }
}
