package com.xaxage.be.acceptance;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-01 Pokémon Enumeration — see {@code doc/specs/pokemon-enumeration.md}.
 *
 * <p>PokeAPI is replaced by an in-process HTTP stub that serves a catalogue defined inline by each test.
 * The application is pointed at it through the {@code pokeapi.base-url} property.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("US-01 Pokémon Enumeration")
class PokemonEnumerationAcceptanceIT {

    private static final StubPokeApi pokeApi = new StubPokeApi();

    @Autowired
    private MockMvcTester mvc;

    @BeforeAll
    static void startPokeApi() throws IOException {
        pokeApi.start();
    }

    @AfterAll
    static void stopPokeApi() {
        pokeApi.stop();
    }

    @DynamicPropertySource
    static void pointAtStubPokeApi(DynamicPropertyRegistry registry) {
        registry.add("pokeapi.base-url", pokeApi::baseUrl);
    }

    @Nested
    @DisplayName("Must return Pokémon one page at a time, in PokeAPI order (by id), including alternate forms")
    class ReturnsPokemonOnePageAtATimeInPokeApiOrder {

        @Test
        @DisplayName("The one where no page or size is given, so page 0 of 20 comes back: #1 Bulbasaur … #20 Raticate, with a next page")
        void defaultsToFirstPageOfTwenty() {
            pokeApi.serves(List.of(
                    pokemon(1, "bulbasaur"), pokemon(2, "ivysaur"), pokemon(3, "venusaur"),
                    pokemon(4, "charmander"), pokemon(5, "charmeleon"), pokemon(6, "charizard"),
                    pokemon(7, "squirtle"), pokemon(8, "wartortle"), pokemon(9, "blastoise"),
                    pokemon(10, "caterpie"), pokemon(11, "metapod"), pokemon(12, "butterfree"),
                    pokemon(13, "weedle"), pokemon(14, "kakuna"), pokemon(15, "beedrill"),
                    pokemon(16, "pidgey"), pokemon(17, "pidgeotto"), pokemon(18, "pidgeot"),
                    pokemon(19, "rattata"), pokemon(20, "raticate"),
                    pokemon(21, "spearow")));

            var response = mvc.get().uri("/api/pokemon").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].id").asArray()
                    .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
            assertThat(response).bodyJson().extractingPath("$.content[0].name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.content[19].name").isEqualTo("raticate");
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(true);
        }

        @Test
        @DisplayName("The one where page 1 of size 20 comes back: #21 Spearow … #40 Wigglytuff, with a next page")
        void returnsSecondPageOfTwenty() {
            var catalogue = new ArrayList<StubPokemon>(filler(1, 20));
            catalogue.addAll(List.of(
                    pokemon(21, "spearow"), pokemon(22, "fearow"), pokemon(23, "ekans"),
                    pokemon(24, "arbok"), pokemon(25, "pikachu"), pokemon(26, "raichu"),
                    pokemon(27, "sandshrew"), pokemon(28, "sandslash"), pokemon(29, "nidoran-f"),
                    pokemon(30, "nidorina"), pokemon(31, "nidoqueen"), pokemon(32, "nidoran-m"),
                    pokemon(33, "nidorino"), pokemon(34, "nidoking"), pokemon(35, "clefairy"),
                    pokemon(36, "clefable"), pokemon(37, "vulpix"), pokemon(38, "ninetales"),
                    pokemon(39, "jigglypuff"), pokemon(40, "wigglytuff")));
            catalogue.addAll(filler(41, 41));
            pokeApi.serves(catalogue);

            var response = mvc.get().uri("/api/pokemon").param("page", "1").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].id").asArray()
                    .containsExactly(21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40);
            assertThat(response).bodyJson().extractingPath("$.content[0].name").isEqualTo("spearow");
            assertThat(response).bodyJson().extractingPath("$.content[19].name").isEqualTo("wigglytuff");
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(true);
        }

        @Test
        @DisplayName("The one where page 0 of the maximum size 100 comes back with the first 100 Pokémon, with a next page")
        void returnsFirstHundredAtMaximumSize() {
            pokeApi.serves(filler(1, 101));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "100").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content").asArray().hasSize(100);
            assertThat(response).bodyJson().extractingPath("$.content[0].id").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.content[99].id").isEqualTo(100);
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(true);
        }

        @Test
        @DisplayName("The one where the last page of size 20 holds only the remaining entries, ending with the alternate forms (ids 10001+), and has no next page")
        void lastPageEndsWithAlternateForms() {
            var catalogue = new ArrayList<StubPokemon>(filler(1, 30));
            catalogue.addAll(List.of(
                    form(10001, "deoxys-attack", 386),
                    form(10002, "deoxys-defense", 386),
                    form(10033, "venusaur-mega", 3)));
            pokeApi.serves(catalogue);

            var response = mvc.get().uri("/api/pokemon").param("page", "1").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].id").asArray()
                    .containsExactly(21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 10001, 10002, 10033);
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(false);
        }

        @Test
        @DisplayName("The one where a page past the last page of size 20 comes back with no entries and no next page")
        void pagePastTheLastIsEmpty() {
            var catalogue = new ArrayList<StubPokemon>(filler(1, 30));
            catalogue.addAll(List.of(
                    form(10001, "deoxys-attack", 386),
                    form(10002, "deoxys-defense", 386),
                    form(10033, "venusaur-mega", 3)));
            pokeApi.serves(catalogue);

            var response = mvc.get().uri("/api/pokemon").param("page", "2").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content").asArray().isEmpty();
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(false);
        }

        @Test
        @DisplayName("The one where an alternate form such as Mega Venusaur (id 10033) is listed as its own entry after all base Pokémon, not next to Venusaur")
        void alternateFormIsListedAfterAllBasePokemon() {
            pokeApi.serves(List.of(
                    pokemon(1, "bulbasaur"), pokemon(2, "ivysaur"), pokemon(3, "venusaur"),
                    pokemon(4, "charmander"), pokemon(5, "charmeleon"),
                    form(10033, "venusaur-mega", 3)));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].name").asArray()
                    .containsExactly("bulbasaur", "ivysaur", "venusaur", "charmander", "charmeleon", "venusaur-mega");
            assertThat(response).bodyJson().extractingPath("$.content[5].id").isEqualTo(10033);
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(false);
        }
    }

    private static StubPokemon pokemon(int id, String name) {
        return new StubPokemon(id, name, id);
    }

    private static StubPokemon form(int id, String name, int speciesId) {
        return new StubPokemon(id, name, speciesId);
    }

    /**
     * Base Pokémon whose names the example does not care about.
     */
    private static List<StubPokemon> filler(int fromId, int toId) {
        return IntStream.rangeClosed(fromId, toId).mapToObj(id -> pokemon(id, "pokemon-" + id)).toList();
    }

    private record StubPokemon(int id, String name, int speciesId) {
    }

    /**
     * Minimal stand-in for PokeAPI v2: the paginated {@code /pokemon} list, {@code /pokemon/{id or name}}
     * and {@code /pokemon-species/{id}}, all served from the catalogue the current test provides.
     */
    private static final class StubPokeApi {

        private final AtomicReference<List<StubPokemon>> catalogue = new AtomicReference<>(List.of());
        private HttpServer server;

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v2/pokemon/", this::pokemon);
            server.createContext("/api/v2/pokemon-species/", this::species);
            server.start();
        }

        void stop() {
            server.stop(0);
        }

        String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort() + "/api/v2";
        }

        void serves(List<StubPokemon> pokemon) {
            catalogue.set(List.copyOf(pokemon));
        }

        private void pokemon(HttpExchange exchange) throws IOException {
            var key = lastPathSegment(exchange.getRequestURI(), "/api/v2/pokemon/");
            if (key.isEmpty()) {
                list(exchange);
                return;
            }
            var found = catalogue.get().stream()
                    .filter(p -> key.equals(String.valueOf(p.id())) || key.equals(p.name()))
                    .findFirst();
            if (found.isEmpty()) {
                respond(exchange, 404, "Not Found");
                return;
            }
            var p = found.get();
            respond(exchange, 200, """
                    {"id":%d,"name":"%s","weight":0,"sprites":{"front_default":null},\
                    "species":{"name":"%s","url":"%s/pokemon-species/%d/"},"moves":[]}"""
                    .formatted(p.id(), p.name(), p.name(), baseUrl(), p.speciesId()));
        }

        private void list(HttpExchange exchange) throws IOException {
            var query = queryParams(exchange.getRequestURI());
            int offset = Integer.parseInt(query.getOrDefault("offset", "0"));
            int limit = Integer.parseInt(query.getOrDefault("limit", "20"));
            var all = catalogue.get();
            var slice = all.subList(Math.min(offset, all.size()), Math.min(offset + limit, all.size()));
            var next = offset + limit < all.size()
                    ? "\"%s/pokemon/?offset=%d&limit=%d\"".formatted(baseUrl(), offset + limit, limit)
                    : "null";
            var previous = offset > 0
                    ? "\"%s/pokemon/?offset=%d&limit=%d\"".formatted(baseUrl(), Math.max(0, offset - limit), limit)
                    : "null";
            var results = slice.stream()
                    .map(p -> "{\"name\":\"%s\",\"url\":\"%s/pokemon/%d/\"}".formatted(p.name(), baseUrl(), p.id()))
                    .collect(Collectors.joining(","));
            respond(exchange, 200, "{\"count\":%d,\"next\":%s,\"previous\":%s,\"results\":[%s]}"
                    .formatted(all.size(), next, previous, results));
        }

        private void species(HttpExchange exchange) throws IOException {
            var key = lastPathSegment(exchange.getRequestURI(), "/api/v2/pokemon-species/");
            respond(exchange, 200, "{\"id\":%s,\"genera\":[]}".formatted(key));
        }

        private static String lastPathSegment(URI uri, String prefix) {
            var rest = uri.getPath().substring(prefix.length());
            return rest.endsWith("/") ? rest.substring(0, rest.length() - 1) : rest;
        }

        private static Map<String, String> queryParams(URI uri) {
            var params = new HashMap<String, String>();
            if (uri.getRawQuery() != null) {
                for (var pair : uri.getRawQuery().split("&")) {
                    var kv = pair.split("=", 2);
                    params.put(kv[0], kv.length > 1 ? kv[1] : "");
                }
            }
            return params;
        }

        private static void respond(HttpExchange exchange, int status, String body) throws IOException {
            var bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}