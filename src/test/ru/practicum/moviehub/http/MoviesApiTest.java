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
        HttpResponse<String> resp = get("/movies");

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies должен вернуть 200"
        );

        assertEquals(
                "application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse(""),
                "Content-Type должен содержать формат данных и кодировку"
        );

        List<Movie> movies = parseMovies(resp);

        assertTrue(
                movies.isEmpty(),
                "Ожидается пустой список фильмов"
        );
    }

    @Test
    void getMovies_whenStoreHasMovie_returnsMovie() throws Exception {
        store.add(new Movie("Interstellar", 2014));

        HttpResponse<String> resp = get("/movies");

        assertEquals(200, resp.statusCode());

        List<Movie> movies = parseMovies(resp);

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
    void getMovieById_whenIdInvalid_returnsBadRequest() throws Exception {
        HttpResponse<String> resp = get("/movies/abc");

        assertEquals(
                400,
                resp.statusCode(),
                "GET /movies/{id} с некорректным id должен вернуть 400"
        );

        ErrorResponse error = parseError(resp);

        assertEquals(
                "Некорректный идентификатор фильма",
                error.getError()
        );
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        store.add(new Movie("Interstellar", 2014));

        HttpResponse<String> resp = get("/movies/1");

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies/{id} должен вернуть 200 для существующего фильма"
        );

        Movie movie = parseMovie(resp);

        assertEquals(
                1,
                movie.getId(),
                "Ответ должен содержать фильм с запрошенным идентификатором"
        );

        assertEquals(
                "Interstellar",
                movie.getTitle(),
                "Ответ должен содержать найденный фильм"
        );
    }

    @Test
    void getMovieById_whenNotExists_returnsNotFound() throws Exception {
        HttpResponse<String> resp = get("/movies/999");

        assertEquals(
                404,
                resp.statusCode(),
                "GET /movies/{id} должен вернуть 404 для несуществующего фильма"
        );

        ErrorResponse error = parseError(resp);

        assertEquals(
                "Фильм не найден",
                error.getError()
        );
    }

    @Test
    void getMovies_whenYearSpecified_returnsMoviesFromThatYear() throws Exception {
        store.add(new Movie("Interstellar", 2014));
        store.add(new Movie("Inception", 2010));
        store.add(new Movie("Whiplash", 2014));

        HttpResponse<String> resp = get("/movies?year=2014");

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies?year=YYYY должен вернуть 200"
        );

        List<Movie> movies = parseMovies(resp);

        assertEquals(
                2,
                movies.size(),
                "Ответ должен содержать только фильмы указанного года"
        );

        assertTrue(
                movies.stream().allMatch(movie -> movie.getYear() == 2014),
                "Все возвращенные фильмы должны соответствовать указанному году"
        );
    }

    @Test
    void getMovies_whenYearHasNoMovies_returnsEmptyArray() throws Exception {
        store.add(new Movie("Interstellar", 2014));

        HttpResponse<String> resp = get("/movies?year=2000");

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies?year=YYYY должен вернуть 200"
        );

        List<Movie> movies = parseMovies(resp);

        assertTrue(
                movies.isEmpty(),
                "Если фильмов указанного года нет, должен вернуться пустой список"
        );
    }

    @Test
    void getMovies_whenYearInvalid_returnsBadRequest() throws Exception {
        HttpResponse<String> resp = get("/movies?year=abc");

        assertEquals(
                400,
                resp.statusCode(),
                "GET /movies с некорректным параметром year должен вернуть 400"
        );

        ErrorResponse error = parseError(resp);

        assertEquals(
                "Некорректный параметр запроса - 'year'",
                error.getError()
        );
    }

    @Test
    void getMovies_whenYearParameterMissing_returnsBadRequest() throws Exception {
        HttpResponse<String> resp = get("/movies?foo=2014");

        assertEquals(
                400,
                resp.statusCode(),
                "GET /movies с некорректным параметром должен вернуть 400"
        );

        ErrorResponse error = parseError(resp);

        assertEquals(
                "Некорректный параметр запроса - 'year'",
                error.getError()
        );
    }

    @Test
    void postMovie_whenValid_addsMovie() throws Exception {
        String json = "{\n" +
                      "  \"title\": \"Interstellar\",\n" +
                      "  \"year\": 2014\n" +
                      "}\n";

        HttpResponse<String> resp = post(json, "application/json");

        assertEquals(
                201,
                resp.statusCode(),
                "POST /movies должен вернуть 201"
        );

        Movie createdMovie = parseMovie(resp);

        assertEquals(
                1,
                createdMovie.getId(),
                "Созданному фильму должен быть присвоен идентификатор"
        );

        assertEquals(
                "Interstellar",
                createdMovie.getTitle(),
                "POST /movies должен вернуть созданный фильм"
        );

        HttpResponse<String> getResp = get("/movies");

        List<Movie> movies = parseMovies(getResp);

        assertEquals(
                1,
                movies.size(),
                "GET /movies должен вернуть добавленный фильм"
        );

        assertEquals(
                1,
                movies.getFirst().getId(),
                "Сохраненный фильм должен иметь присвоенный идентификатор"
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
                             "  \"title\": \"Interstellar\",\n" +
                             "  \"year\":\n" +
                             "}\n";

        HttpResponse<String> resp = post(invalidJson, "application/json");

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

        ErrorResponse error = parseError(resp);

        assertEquals(
                "Некорректный JSON",
                error.getError()
        );
    }

    @Test
    void postMovie_whenTitleEmpty_returnsUnprocessableEntity() throws Exception {
        String json = "{\n" +
                      "  \"title\": \"\",\n" +
                      "  \"year\": 2014\n" +
                      "}\n";

        HttpResponse<String> resp = post(json, "application/json");

        assertEquals(
                422,
                resp.statusCode(),
                "POST /movies с ошибками валидации должен вернуть 422"
        );

        ErrorResponse error = parseError(resp);

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

        HttpResponse<String> resp = post(json, "application/json");

        assertEquals(
                422,
                resp.statusCode(),
                "POST /movies с ошибками валидации должен вернуть 422"
        );

        ErrorResponse error = parseError(resp);

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
    void postMovie_whenMultipleFieldsInvalid_returnsAllValidationErrors() throws Exception {
        String json = "{\n" +
                      "  \"title\": \"\",\n" +
                      "  \"year\": 0\n" +
                      "}\n";

        HttpResponse<String> resp = post(json, "application/json");

        assertEquals(
                422,
                resp.statusCode(),
                "POST /movies с ошибками валидации должен вернуть 422"
        );

        ErrorResponse error = parseError(resp);

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

        HttpResponse<String> resp = post(json, "application/json");

        assertEquals(
                422,
                resp.statusCode(),
                "Название длиннее 100 символов должно вернуть 422"
        );

        ErrorResponse error = parseError(resp);

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

        HttpResponse<String> resp = post(json, "application/json");

        assertEquals(
                422,
                resp.statusCode(),
                "Слишком поздний год выпуска должен вернуть 422"
        );

        ErrorResponse error = parseError(resp);

        assertEquals(
                List.of("Год выпуска не должен быть позже следующего года"),
                error.getDetails()
        );
    }

    @Test
    void postMovie_whenValid_assignsId() throws Exception {
        String json = "{\n" +
                      "  \"title\": \"Interstellar\",\n" +
                      "  \"year\": 2014\n" +
                      "}\n";

        HttpResponse<String> resp = post(json, "application/json");

        assertEquals(
                201,
                resp.statusCode(),
                "POST /movies должен вернуть 201"
        );

        Movie movie = parseMovie(resp);

        assertEquals(
                1,
                movie.getId(),
                "Сервер должен присвоить фильму идентификатор"
        );
    }

    @Test
    void postMovies_whenMultipleMoviesAdded_assignsSequentialIds() throws Exception {
        String firstJson = "{\n" +
                           "  \"title\": \"Interstellar\",\n" +
                           "  \"year\": 2014\n" +
                           "}\n";

        String secondJson = "{\n" +
                            "  \"title\": \"Inception\",\n" +
                            "  \"year\": 2010\n" +
                            "}\n";

        Movie firstMovie = parseMovie(post(firstJson, "application/json"));
        Movie secondMovie = parseMovie(post(secondJson, "application/json"));

        assertEquals(
                1,
                firstMovie.getId(),
                "Первому фильму должен быть присвоен id = 1"
        );

        assertEquals(
                2,
                secondMovie.getId(),
                "Второму фильму должен быть присвоен id = 2"
        );
    }

    @Test
    void postMovie_whenContentTypeInvalid_returnsUnsupportedMediaType() throws Exception {
        String json = "{\n" +
                      "  \"title\": \"Interstellar\",\n" +
                      "  \"year\": 2014\n" +
                      "}\n";

        HttpResponse<String> resp = post(json, "text/plain");

        assertEquals(
                415,
                resp.statusCode(),
                "POST /movies с неверным Content-Type должен вернуть 415"
        );

        ErrorResponse error = parseError(resp);

        assertEquals(
                "Неподдерживаемый тип данных",
                error.getError()
        );
    }

    @Test
    void postMovie_whenContentTypeContainsCharset_addsMovie() throws Exception {
        String json = "{\n" +
                      "  \"title\": \"Interstellar\",\n" +
                      "  \"year\": 2014\n" +
                      "}\n";

        HttpResponse<String> resp = post(
                json,
                "application/json; charset=UTF-8"
        );

        assertEquals(
                201,
                resp.statusCode(),
                "POST /movies с JSON и кодировкой UTF-8 должен вернуть 201"
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

        ErrorResponse error = parseError(resp);

        assertEquals(
                "Метод не поддерживается",
                error.getError()
        );
    }

    @Test
    void deleteMovie_whenExists_removesMovie() throws Exception {
        store.add(new Movie("Interstellar", 2014));

        HttpResponse<String> resp = delete("/movies/1");

        assertEquals(
                204,
                resp.statusCode(),
                "DELETE /movies/{id} должен вернуть 204"
        );

        HttpResponse<String> getResp = get("/movies/1");

        assertEquals(
                404,
                getResp.statusCode(),
                "Удаленный фильм не должен находиться"
        );
    }

    @Test
    void deleteMovie_whenNotExists_returnsNotFound() throws Exception {
        HttpResponse<String> resp = delete("/movies/999");

        assertEquals(
                404,
                resp.statusCode(),
                "DELETE /movies/{id} должен вернуть 404 для несуществующего фильма"
        );

        ErrorResponse error = parseError(resp);

        assertEquals(
                "Фильм не найден",
                error.getError()
        );
    }

    @Test
    void deleteMovie_whenIdInvalid_returnsBadRequest() throws Exception {
        HttpResponse<String> resp = delete("/movies/abc");

        assertEquals(
                400,
                resp.statusCode(),
                "DELETE /movies/{id} с некорректным id должен вернуть 400"
        );

        ErrorResponse error = parseError(resp);

        assertEquals(
                "Некорректный идентификатор фильма",
                error.getError()
        );
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        return send(request);
    }

    private HttpResponse<String> post(String json, String contentType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", contentType)
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.ofString(
                        json,
                        StandardCharsets.UTF_8
                ))
                .build();

        return send(request);
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(2))
                .DELETE()
                .build();

        return send(request);
    }

    private Movie parseMovie(HttpResponse<String> response) {
        return gson.fromJson(response.body(), Movie.class);
    }

    private List<Movie> parseMovies(HttpResponse<String> response) {
        return gson.fromJson(
                response.body(),
                new ListOfMoviesTypeToken().getType()
        );
    }

    private ErrorResponse parseError(HttpResponse<String> response) {
        return gson.fromJson(response.body(), ErrorResponse.class);
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }
}
