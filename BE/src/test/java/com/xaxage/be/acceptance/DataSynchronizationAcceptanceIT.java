package com.xaxage.be.acceptance;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-03 Data Synchronization — see {@code doc/specs/data-synchronization.md}.
 *
 * <p>PokeAPI is replaced by an in-process HTTP stub that serves a catalogue defined inline by each test.
 * The local database starts every test empty.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("US-03 Data Synchronization")
class DataSynchronizationAcceptanceIT {

    private static final StubPokeApi pokeApi = new StubPokeApi();

    private static final String BULBASAUR_SPRITE =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient database;

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

    @BeforeEach
    void startWithAnEmptyLocalDatabase() {
        database.sql("DELETE FROM stored_pokemon_tag").update();
        database.sql("DELETE FROM stored_pokemon").update();
    }

    @Nested
    @DisplayName("Must copy a Pokémon from PokeAPI into the local database, with our own fields empty")
    class CopiesAPokemonFromPokeApiIntoTheLocalDatabase {

        @Test
        @DisplayName("The one where Bulbasaur (id 1) is synchronised, so the local copy has id 1, name \"bulbasaur\", "
                + "category \"Seed Pokémon\", weight 69, its sprite, no localized name, no region and no tags (201)")
        void bulbasaurIsCopiedWithOurOwnFieldsEmpty() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur", "Seed Pokémon", 69, BULBASAUR_SPRITE)));

            var response = mvc.post().uri("/api/stored-pokemon/{id}/sync", 1).exchange();

            assertThat(response).hasStatus(HttpStatus.CREATED);
            assertThat(response).bodyJson().extractingPath("$.id").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.category").isEqualTo("Seed Pokémon");
            assertThat(response).bodyJson().extractingPath("$.weight").isEqualTo(69);
            assertThat(response).bodyJson().extractingPath("$.sprite").isEqualTo(BULBASAUR_SPRITE);
            assertThat(response).bodyJson().hasPath("$.localizedName");
            assertThat(response).bodyJson().extractingPath("$.localizedName").isNull();
            assertThat(response).bodyJson().hasPath("$.region");
            assertThat(response).bodyJson().extractingPath("$.region").isNull();
            assertThat(response).bodyJson().extractingPath("$.tags").asArray().isEmpty();
        }

        @Test
        @DisplayName("The one where PokeAPI has no Pokémon with that id, so nothing is stored and the reader gets a 404 "
                + "with the standard error body")
        void unknownPokemonIsNotStored() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur", "Seed Pokémon", 69, BULBASAUR_SPRITE)));

            var response = mvc.post().uri("/api/stored-pokemon/{id}/sync", 99999).exchange();

            assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(404);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Not Found");
            assertThat(database.sql("SELECT COUNT(*) FROM stored_pokemon").query(Integer.class).single())
                    .isZero();
        }
    }

    @Nested
    @DisplayName("Must refresh PokeAPI's fields when a stored Pokémon is synchronised again, keeping our own fields")
    class RefreshesPokeApisFieldsKeepingOurOwn {

        @Test
        @DisplayName("The one where Bulbasaur already has localized name \"Bisasam\" and region \"Kanto\"; synchronising "
                + "it again updates the PokeAPI fields and keeps \"Bisasam\" and \"Kanto\" (200)")
        void resynchronisingBulbasaurUpdatesPokeApisFieldsAndKeepsOurOwn() {
            database.sql("""
                            INSERT INTO stored_pokemon (id, name, category, weight, sprite, localized_name, region)
                            VALUES (1, 'bulbasaur', 'Seed Pokémon', 60, 'outdated-bulbasaur.png', 'Bisasam', 'Kanto')""")
                    .update();
            pokeApi.serves(List.of(pokemon(1, "bulbasaur", "Seed Pokémon", 69, BULBASAUR_SPRITE)));

            var response = mvc.post().uri("/api/stored-pokemon/{id}/sync", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.id").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.category").isEqualTo("Seed Pokémon");
            assertThat(response).bodyJson().extractingPath("$.weight").isEqualTo(69);
            assertThat(response).bodyJson().extractingPath("$.sprite").isEqualTo(BULBASAUR_SPRITE);
            assertThat(response).bodyJson().extractingPath("$.localizedName").isEqualTo("Bisasam");
            assertThat(response).bodyJson().extractingPath("$.region").isEqualTo("Kanto");
        }
    }

    @Nested
    @DisplayName("Must list stored Pokémon one page at a time, ordered by id")
    class ListsStoredPokemonOnePageAtATime {

        @Test
        @DisplayName("The one where 3 Pokémon are stored and page 0 of size 20 returns all 3, with the total and total pages")
        void firstPageReturnsAllThreeStoredPokemonOrderedById() {
            stores(7, "squirtle", "Tiny Turtle Pokémon", 90);
            stores(1, "bulbasaur", "Seed Pokémon", 69);
            stores(4, "charmander", "Lizard Pokémon", 85);

            var response = mvc.get().uri("/api/stored-pokemon?page=0&size=20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(1, 4, 7);
            assertThat(response).bodyJson().extractingPath("$.content[*].name").asArray()
                    .containsExactly("bulbasaur", "charmander", "squirtle");
            assertThat(response).bodyJson().extractingPath("$.count").isEqualTo(3);
            assertThat(response).bodyJson().extractingPath("$.totalPages").isEqualTo(1);
        }

        @Test
        @DisplayName("The one where nothing is stored, so the page is empty with a total of 0")
        void nothingStoredGivesAnEmptyPage() {
            var response = mvc.get().uri("/api/stored-pokemon?page=0&size=20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content").asArray().isEmpty();
            assertThat(response).bodyJson().extractingPath("$.count").isEqualTo(0);
        }
    }

    private void stores(int id, String name, String category, int weight) {
        database.sql("""
                        INSERT INTO stored_pokemon (id, name, category, weight, sprite, localized_name, region)
                        VALUES (?, ?, ?, ?, ?, NULL, NULL)""")
                .params(id, name, category, weight, name + ".png")
                .update();
    }

    private static StubPokemon pokemon(int id, String name, String category, int weight, String sprite) {
        return new StubPokemon(id, name, category, weight, sprite);
    }

    /**
     * @param category the English genus of the Pokémon's species
     * @param weight   in hectograms, as PokeAPI gives it
     * @param sprite   the default front sprite URL
     */
    private record StubPokemon(int id, String name, String category, int weight, String sprite) {
    }

    /**
     * Minimal stand-in for PokeAPI v2's {@code /pokemon/{id}} and {@code /pokemon-species/{id}}, served from the
     * catalogue the current test provides. Every Pokémon is its own species.
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
            var found = find(lastPathSegment(exchange.getRequestURI(), "/api/v2/pokemon/"));
            if (found.isEmpty()) {
                respond(exchange, 404, "Not Found");
                return;
            }
            var p = found.get();
            respond(exchange, 200, """
                    {"id":%d,"name":"%s","weight":%d,\
                    "sprites":{"front_default":"%s","other":{"official-artwork":{"front_default":null}}},\
                    "species":{"name":"%s","url":"%s/pokemon-species/%d/"},\
                    "moves":[],"stats":[]}"""
                    .formatted(p.id(), p.name(), p.weight(), p.sprite(), p.name(), baseUrl(), p.id()));
        }

        private void species(HttpExchange exchange) throws IOException {
            var found = find(lastPathSegment(exchange.getRequestURI(), "/api/v2/pokemon-species/"));
            if (found.isEmpty()) {
                respond(exchange, 404, "Not Found");
                return;
            }
            var p = found.get();
            respond(exchange, 200, """
                    {"id":%d,"name":"%s",\
                    "genera":[{"genus":"%s","language":{"name":"en","url":"%s/language/9/"}}],\
                    "flavor_text_entries":[],"evolution_chain":null}"""
                    .formatted(p.id(), p.name(), p.category(), baseUrl()));
        }

        private Optional<StubPokemon> find(String id) {
            return catalogue.get().stream()
                    .filter(p -> id.equals(String.valueOf(p.id())))
                    .findFirst();
        }

        private static String lastPathSegment(URI uri, String prefix) {
            var rest = uri.getPath().substring(prefix.length());
            return rest.endsWith("/") ? rest.substring(0, rest.length() - 1) : rest;
        }

        private void respond(HttpExchange exchange, int status, String body) throws IOException {
            var bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}
