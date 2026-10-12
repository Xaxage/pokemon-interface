package com.xaxage.be.acceptance;

import com.github.benmanes.caffeine.cache.Ticker;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-02 Detailed View — see {@code doc/specs/pokemon-detailed-view.md}.
 *
 * <p>PokeAPI is replaced by an in-process HTTP stub that serves a catalogue defined inline by each test.
 * The application is pointed at it through the {@code pokeapi.base-url} property.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PokemonDetailedViewAcceptanceIT.FakeClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("US-02 Detailed View")
class PokemonDetailedViewAcceptanceIT {

    private static final StubPokeApi pokeApi = new StubPokeApi();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private FakeTicker clock;

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
    @DisplayName("Must show a chosen Pokémon by its id")
    class ShowsAChosenPokemonByItsId {

        @Test
        @DisplayName("The one where id 1 returns Bulbasaur")
        void idOneReturnsBulbasaur() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur"), pokemon(2, "ivysaur")));

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.id").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("bulbasaur");
        }

        @Test
        @DisplayName("The one where no Pokémon has the id (e.g. 99999), so the reader gets a 404 with the standard error body")
        void unknownIdIsNotFound() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur")));

            var response = mvc.get().uri("/api/pokemon/{id}", 99999).exchange();

            assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(404);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Not Found");
        }
    }

    @Nested
    @DisplayName("Must show the Pokémon's official artwork as its image")
    class ShowsTheOfficialArtworkAsItsImage {

        @Test
        @DisplayName("The one where Bulbasaur comes back with the link to its official artwork")
        void bulbasaurComesBackWithItsOfficialArtwork() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur")
                    .withSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png")
                    .withArtwork("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png")));

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.image")
                    .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/1.png");
        }

        @Test
        @DisplayName("The one where a Pokémon has no official artwork, so its default front sprite is shown instead")
        void pokemonWithoutOfficialArtworkShowsItsDefaultFrontSprite() {
            pokeApi.serves(List.of(pokemon(10080, "pikachu-rock-star")
                    .withSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/10080.png")));

            var response = mvc.get().uri("/api/pokemon/{id}", 10080).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.image")
                    .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/10080.png");
        }
    }

    @Nested
    @DisplayName("Must show the six core stats with their base values")
    class ShowsTheSixCoreStatsWithTheirBaseValues {

        @Test
        @DisplayName("The one where Bulbasaur shows hp 45, attack 49, defense 49, special-attack 65, special-defense 65 and speed 45")
        void bulbasaurShowsItsSixCoreStats() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur").withStats(
                    stat("hp", 45), stat("attack", 49), stat("defense", 49),
                    stat("special-attack", 65), stat("special-defense", 65), stat("speed", 45))));

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.stats[*].name").asArray()
                    .containsExactly("hp", "attack", "defense", "special-attack", "special-defense", "speed");
            assertThat(response).bodyJson().extractingPath("$.stats[*].base").asArray()
                    .containsExactly(45, 49, 49, 65, 65, 45);
        }
    }

    @Nested
    @DisplayName("Must show an English description from the Pokémon's species, taken from the newest game that has one")
    class ShowsAnEnglishDescriptionFromTheNewestGame {

        @Test
        @DisplayName("The one where Bulbasaur's description is a single line of plain text, with PokeAPI's line breaks replaced by spaces")
        void bulbasaursDescriptionIsASingleLineOfPlainText() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur").withDescriptions(
                    describedIn("red", "en", "A strange seed was\nplanted on its\nback at birth.\fThe plant sprouts\nand grows with\nthis POKéMON."),
                    describedIn("shield", "en", "There is a plant seed on its back right from\nthe day this Pokémon is born. The seed slowly\ngrows larger."),
                    describedIn("shield", "ja", "うまれたときから　せなかに\nしょくぶつの　タネが　あって\nすこしずつ　おおきく　そだつ。"))));

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.description").isEqualTo(
                    "There is a plant seed on its back right from the day this Pokémon is born. The seed slowly grows larger.");
        }

        @Test
        @DisplayName("The one where a species has no English description, so the description is empty")
        void speciesWithoutEnglishDescriptionHasAnEmptyDescription() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur").withDescriptions(
                    describedIn("shield", "ja", "うまれたときから　せなかに\nしょくぶつの　タネが　あって\nすこしずつ　おおきく　そだつ。"))));

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().hasPath("$.description");
            assertThat(response).bodyJson().extractingPath("$.description").isNull();
        }
    }

    @Nested
    @DisplayName("Must show the whole evolution chain, from the first stage to the last")
    class ShowsTheWholeEvolutionChain {

        @Test
        @DisplayName("The one where Ivysaur (middle stage) shows Bulbasaur → Ivysaur → Venusaur")
        void middleStageShowsTheChainFromFirstToLastStage() {
            pokeApi.serves(List.of(pokemon(2, "ivysaur")
                    .withEvolutionChain(evolves("bulbasaur", evolves("ivysaur", evolves("venusaur"))))));

            var response = mvc.get().uri("/api/pokemon/{id}", 2).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.evolvesTo[*].name").asArray()
                    .containsExactly("ivysaur");
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.evolvesTo[0].evolvesTo[*].name").asArray()
                    .containsExactly("venusaur");
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.evolvesTo[0].evolvesTo[0].evolvesTo")
                    .asArray().isEmpty();
        }

        @Test
        @DisplayName("The one where Eevee (branching) shows Eevee → Vaporeon, Jolteon, Flareon, … (each as a second stage)")
        void branchingChainShowsEachBranchAsASecondStage() {
            pokeApi.serves(List.of(pokemon(133, "eevee").withEvolutionChain(evolves("eevee",
                    evolves("vaporeon"), evolves("jolteon"), evolves("flareon"), evolves("espeon"),
                    evolves("umbreon"), evolves("leafeon"), evolves("glaceon"), evolves("sylveon")))));

            var response = mvc.get().uri("/api/pokemon/{id}", 133).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.name").isEqualTo("eevee");
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.evolvesTo[*].name").asArray()
                    .containsExactly("vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon");
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.evolvesTo[*].evolvesTo[*]").asArray()
                    .isEmpty();
        }

        @Test
        @DisplayName("The one where Tauros (does not evolve) shows Tauros only")
        void pokemonThatDoesNotEvolveShowsOnlyItself() {
            pokeApi.serves(List.of(pokemon(128, "tauros").withEvolutionChain(evolves("tauros"))));

            var response = mvc.get().uri("/api/pokemon/{id}", 128).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.name").isEqualTo("tauros");
            assertThat(response).bodyJson().extractingPath("$.evolutionChain.evolvesTo").asArray().isEmpty();
        }
    }

    @Nested
    @DisplayName("Must reject an id that is not a positive whole number (400, with the standard error body)")
    class RejectsAnIdThatIsNotAPositiveWholeNumber {

        @ParameterizedTest(name = "The one where id {0} is rejected")
        @ValueSource(strings = {"0", "-1", "abc"})
        @DisplayName("The one where an id that is not a positive whole number is rejected with the standard error body")
        void rejectsAnIdThatIsNotAPositiveWholeNumber(String id) {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur")));

            var response = mvc.get().uri("/api/pokemon/{id}", id).exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(400);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
        }
    }

    @Nested
    @DisplayName("Must let anyone view details, signed in or not")
    class LetsAnyoneViewDetails {

        @Test
        @DisplayName("The one where a visitor with no token views Bulbasaur")
        void visitorWithNoTokenViewsBulbasaur() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur")));

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("bulbasaur");
        }
    }

    @Nested
    @DisplayName("Should reuse PokeAPI responses for 24 hours and keep serving them while PokeAPI is unavailable")
    class ReusesPokeApiResponsesFor24Hours {

        @Test
        @DisplayName("The one where Bulbasaur is viewed twice within 24 hours, and PokeAPI is called only the first time")
        void bulbasaurViewedTwiceWithin24HoursCallsPokeApiOnlyTheFirstTime() throws Exception {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur")));
            var first = mvc.get().uri("/api/pokemon/{id}", 1).exchange();
            var callsAfterFirstView = pokeApi.requestsReceived();

            clock.advance(Duration.ofHours(23));
            var second = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(callsAfterFirstView).isPositive();
            assertThat(pokeApi.requestsReceived()).isEqualTo(callsAfterFirstView);
            assertThat(second).hasStatusOk();
            assertThat(second).bodyJson().isEqualTo(first.getResponse().getContentAsString());
        }

        @Test
        @DisplayName("The one where PokeAPI is down and the Pokémon is not cached, so the reader gets a 503 with the standard error body and no upstream details")
        void failsClearlyWhenPokeApiIsDownAndThePokemonIsNotCached() throws Exception {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur")));
            pokeApi.goesDown();

            var response = mvc.get().uri("/api/pokemon/{id}", 1).exchange();

            assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(503);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Service Unavailable");
            assertThat(response.getResponse().getContentAsString())
                    .doesNotContain(pokeApi.baseUrl(), "localhost", "pokeapi-db-7", "Exception");
        }
    }

    private static StubPokemon pokemon(int id, String name) {
        return new StubPokemon(id, name, null, null, List.of(), List.of(), evolves(name));
    }

    private static StubEvolution evolves(String species, StubEvolution... into) {
        return new StubEvolution(species, List.of(into));
    }

    private static StubStat stat(String name, int base) {
        return new StubStat(name, base);
    }

    private static StubFlavorText describedIn(String version, String language, String text) {
        return new StubFlavorText(version, language, text);
    }

    /**
     * @param sprite       the default front sprite URL, or {@code null} when PokeAPI has none
     * @param artwork      the official artwork URL, or {@code null} when PokeAPI has none
     * @param stats        the Pokémon's stats with their base values, in PokeAPI order
     * @param descriptions the flavor texts of the species whose id is this Pokémon's id, in every game and language
     * @param evolution    the whole evolution chain of that species, from its first stage; just the Pokémon itself
     *                     unless the test says otherwise
     */
    private record StubPokemon(int id, String name, String sprite, String artwork, List<StubStat> stats,
                               List<StubFlavorText> descriptions, StubEvolution evolution) {

        StubPokemon withSprite(String spriteUrl) {
            return new StubPokemon(id, name, spriteUrl, artwork, stats, descriptions, evolution);
        }

        StubPokemon withArtwork(String artworkUrl) {
            return new StubPokemon(id, name, sprite, artworkUrl, stats, descriptions, evolution);
        }

        StubPokemon withStats(StubStat... baseStats) {
            return new StubPokemon(id, name, sprite, artwork, List.of(baseStats), descriptions, evolution);
        }

        StubPokemon withDescriptions(StubFlavorText... flavorTexts) {
            return new StubPokemon(id, name, sprite, artwork, stats, List.of(flavorTexts), evolution);
        }

        StubPokemon withEvolutionChain(StubEvolution firstStage) {
            return new StubPokemon(id, name, sprite, artwork, stats, descriptions, firstStage);
        }
    }

    /**
     * One stage of an evolution chain and the species it evolves into next.
     */
    private record StubEvolution(String species, List<StubEvolution> into) {
    }

    private record StubStat(String name, int base) {
    }

    /**
     * @param version the game the text appears in
     */
    private record StubFlavorText(String version, String language, String text) {
    }

    /**
     * Minimal stand-in for PokeAPI v2's {@code /pokemon/{id}} and {@code /pokemon-species/{id}}, served from the
     * catalogue the current test provides. The known games (versions), oldest first, are always the same.
     */
    private static final class StubPokeApi {

        private static final List<String> VERSIONS = List.of("red", "blue", "x", "y", "sword", "shield");

        private final AtomicReference<List<StubPokemon>> catalogue = new AtomicReference<>(List.of());
        private final AtomicInteger requestsReceived = new AtomicInteger();
        private final AtomicBoolean down = new AtomicBoolean();
        private HttpServer server;

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v2/pokemon/", whileUp(this::pokemon));
            server.createContext("/api/v2/pokemon-species/", whileUp(this::species));
            server.createContext("/api/v2/evolution-chain/", whileUp(this::evolutionChain));
            server.start();
        }

        int requestsReceived() {
            return requestsReceived.get();
        }

        /**
         * From now on every request fails the way an overloaded upstream does, leaking its internals.
         */
        void goesDown() {
            down.set(true);
        }

        private HttpHandler whileUp(HttpHandler handler) {
            return exchange -> {
                if (down.get()) {
                    respond(exchange, 503, "upstream pokeapi-db-7.internal:5432 refused connection");
                } else {
                    handler.handle(exchange);
                }
            };
        }

        void stop() {
            server.stop(0);
        }

        String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort() + "/api/v2";
        }

        void serves(List<StubPokemon> pokemon) {
            catalogue.set(List.copyOf(pokemon));
            down.set(false);
        }

        private void pokemon(HttpExchange exchange) throws IOException {
            var found = find(lastPathSegment(exchange.getRequestURI(), "/api/v2/pokemon/"));
            if (found.isEmpty()) {
                respond(exchange, 404, "Not Found");
                return;
            }
            var p = found.get();
            respond(exchange, 200, """
                    {"id":%d,"name":"%s",\
                    "sprites":{"front_default":%s,"other":{"official-artwork":{"front_default":%s}}},\
                    "species":{"name":"%s","url":"%s/pokemon-species/%d/"},\
                    "stats":[%s]}"""
                    .formatted(p.id(), p.name(), jsonString(p.sprite()), jsonString(p.artwork()),
                            p.name(), baseUrl(), p.id(), statsJson(p.stats())));
        }

        private void species(HttpExchange exchange) throws IOException {
            var found = find(lastPathSegment(exchange.getRequestURI(), "/api/v2/pokemon-species/"));
            if (found.isEmpty()) {
                respond(exchange, 404, "Not Found");
                return;
            }
            var p = found.get();
            respond(exchange, 200, """
                    {"id":%d,"name":"%s","genera":[],"flavor_text_entries":[%s],\
                    "evolution_chain":{"url":"%s/evolution-chain/%d/"}}"""
                    .formatted(p.id(), p.name(), flavorTextsJson(p.descriptions()), baseUrl(), p.id()));
        }

        /**
         * Serves each Pokémon's chain under the Pokémon's own id, which is the id its species links to.
         */
        private void evolutionChain(HttpExchange exchange) throws IOException {
            var found = find(lastPathSegment(exchange.getRequestURI(), "/api/v2/evolution-chain/"));
            if (found.isEmpty()) {
                respond(exchange, 404, "Not Found");
                return;
            }
            var p = found.get();
            respond(exchange, 200, """
                    {"id":%d,"baby_trigger_item":null,"chain":%s}"""
                    .formatted(p.id(), chainLinkJson(p.evolution())));
        }

        private String chainLinkJson(StubEvolution stage) {
            return """
                    {"is_baby":false,"species":{"name":"%s","url":"%s/pokemon-species/%s/"},\
                    "evolution_details":[],"evolves_to":[%s]}"""
                    .formatted(stage.species(), baseUrl(), stage.species(), stage.into().stream()
                            .map(this::chainLinkJson)
                            .collect(Collectors.joining(",")));
        }

        private Optional<StubPokemon> find(String id) {
            return catalogue.get().stream()
                    .filter(p -> id.equals(String.valueOf(p.id())))
                    .findFirst();
        }

        private String flavorTextsJson(List<StubFlavorText> flavorTexts) {
            return flavorTexts.stream()
                    .map(f -> """
                            {"flavor_text":%s,"language":{"name":"%s","url":"%s/language/%s/"},\
                            "version":{"name":"%s","url":"%s/version/%d/"}}"""
                            .formatted(jsonString(f.text()), f.language(), baseUrl(), f.language(),
                                    f.version(), baseUrl(), VERSIONS.indexOf(f.version()) + 1))
                    .collect(Collectors.joining(","));
        }

        private String statsJson(List<StubStat> stats) {
            return stats.stream()
                    .map(s -> """
                            {"base_stat":%d,"effort":0,"stat":{"name":"%s","url":"%s/stat/%s/"}}"""
                            .formatted(s.base(), s.name(), baseUrl(), s.name()))
                    .collect(Collectors.joining(","));
        }

        private static String jsonString(String value) {
            return value == null ? "null" : "\"" + value.replace("\n", "\\n").replace("\f", "\\f") + "\"";
        }

        private static String lastPathSegment(URI uri, String prefix) {
            var rest = uri.getPath().substring(prefix.length());
            return rest.endsWith("/") ? rest.substring(0, rest.length() - 1) : rest;
        }

        private void respond(HttpExchange exchange, int status, String body) throws IOException {
            requestsReceived.incrementAndGet();
            var bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    /**
     * Lets a test move time forward, so cached PokeAPI responses can age without waiting.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class FakeClockConfig {

        @Bean
        @Primary
        FakeTicker fakeTicker() {
            return new FakeTicker();
        }
    }

    static final class FakeTicker implements Ticker {

        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
