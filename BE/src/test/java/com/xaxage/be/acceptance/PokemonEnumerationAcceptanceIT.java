package com.xaxage.be.acceptance;

import com.github.benmanes.caffeine.cache.Ticker;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
@Import(PokemonEnumerationAcceptanceIT.FakeClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("US-01 Pokémon Enumeration")
class PokemonEnumerationAcceptanceIT {

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

    @Nested
    @DisplayName("Must tell the reader where they are in the full list")
    class TellsTheReaderWhereTheyAreInTheFullList {

        @Test
        @DisplayName("The one where page 0 of size 20 reports page 0, size 20, the total number of Pokémon as reported by PokeAPI, and the total number of pages")
        void firstPageReportsItsPositionAndTheTotals() {
            pokeApi.serves(filler(1, 45));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.page").isEqualTo(0);
            assertThat(response).bodyJson().extractingPath("$.size").isEqualTo(20);
            assertThat(response).bodyJson().extractingPath("$.count").isEqualTo(45);
            assertThat(response).bodyJson().extractingPath("$.totalPages").isEqualTo(3);
        }

        @Test
        @DisplayName("The one where a page past the end comes back empty but still reports the real total and total pages")
        void pagePastTheEndStillReportsTheRealTotals() {
            var catalogue = new ArrayList<StubPokemon>(filler(1, 30));
            catalogue.addAll(List.of(
                    form(10001, "deoxys-attack", 386),
                    form(10002, "deoxys-defense", 386),
                    form(10033, "venusaur-mega", 3)));
            pokeApi.serves(catalogue);

            var response = mvc.get().uri("/api/pokemon").param("page", "2").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content").asArray().isEmpty();
            assertThat(response).bodyJson().extractingPath("$.count").isEqualTo(33);
            assertThat(response).bodyJson().extractingPath("$.totalPages").isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's default front sprite")
    class ShowsEachPokemonsDefaultFrontSprite {

        @Test
        @DisplayName("The one where Bulbasaur comes back with the link to its default front battle sprite")
        void bulbasaurComesBackWithItsDefaultFrontSprite() {
            pokeApi.serves(List.of(
                    pokemon(1, "bulbasaur")
                            .withSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png")));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.content[0].sprite")
                    .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
        }

        @Test
        @DisplayName("The one where an alternate form has no default front sprite. It is still listed, with an empty sprite")
        void alternateFormWithoutDefaultFrontSpriteIsStillListed() {
            pokeApi.serves(List.of(
                    pokemon(1, "bulbasaur")
                            .withSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png"),
                    form(10264, "koraidon-limited-build", 1007)));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(1, 10264);
            assertThat(response).bodyJson().extractingPath("$.content[1].name").isEqualTo("koraidon-limited-build");
            assertThat(response).bodyJson().hasPath("$.content[1].sprite");
            assertThat(response).bodyJson().extractingPath("$.content[1].sprite").isNull();
        }

        @Test
        @DisplayName("The one where PokeAPI has no details for a listed Pokémon. It is still listed, with only its name and an empty sprite")
        void listedPokemonWithoutDetailsIsStillListedWithOnlyItsName() {
            pokeApi.serves(List.of(
                    pokemon(1, "bulbasaur")
                            .withSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png"),
                    pokemon(2, "ivysaur").withoutDetails()));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(1, 2);
            assertThat(response).bodyJson().extractingPath("$.content[1].name").isEqualTo("ivysaur");
            assertThat(response).bodyJson().hasPath("$.content[1].sprite");
            assertThat(response).bodyJson().extractingPath("$.content[1].sprite").isNull();
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's category as its English species genus")
    class ShowsEachPokemonsCategoryAsItsEnglishSpeciesGenus {

        @Test
        @DisplayName("The one where Bulbasaur's category is \"Seed Pokémon\"")
        void bulbasaursCategoryIsSeedPokemon() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur").withCategory("Seed Pokémon")));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.content[0].category").isEqualTo("Seed Pokémon");
        }

        @Test
        @DisplayName("The one where Alolan Raichu takes its species' category, \"Mouse Pokémon\", the same as Raichu")
        void alolanRaichuTakesItsSpeciesCategory() {
            pokeApi.serves(List.of(
                    pokemon(26, "raichu").withCategory("Mouse Pokémon"),
                    form(10100, "raichu-alola", 26)));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].name").asArray()
                    .containsExactly("raichu", "raichu-alola");
            assertThat(response).bodyJson().extractingPath("$.content[0].category").isEqualTo("Mouse Pokémon");
            assertThat(response).bodyJson().extractingPath("$.content[1].category").isEqualTo("Mouse Pokémon");
        }

        @Test
        @DisplayName("The one where a species has no English genus. The Pokémon is still listed, with an empty category")
        void speciesWithoutEnglishGenusIsStillListedWithAnEmptyCategory() {
            pokeApi.serves(List.of(
                    pokemon(1, "bulbasaur").withCategory("Seed Pokémon"),
                    pokemon(2, "ivysaur")));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(1, 2);
            assertThat(response).bodyJson().hasPath("$.content[1].category");
            assertThat(response).bodyJson().extractingPath("$.content[1].category").isNull();
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's weight in hectograms, exactly as PokeAPI gives it")
    class ShowsEachPokemonsWeightInHectograms {

        @ParameterizedTest(name = "The one where {1} is shown weighing {2} hg")
        @CsvSource({
                "1,   bulbasaur,  69",
                "92,  gastly,     1",
                "797, celesteela, 9999"
        })
        @DisplayName("The one where each Pokémon's weight is shown in hectograms, exactly as PokeAPI gives it")
        void showsTheWeightExactlyAsPokeApiGivesIt(int id, String name, int hectograms) {
            pokeApi.serves(List.of(pokemon(id, name).withWeight(hectograms)));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].name").isEqualTo(name);
            assertThat(response).bodyJson().extractingPath("$.content[0].weight").isEqualTo(hectograms);
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's skills (its moves) and how each is learned, in the newest version group known to PokeAPI")
    class ShowsEachPokemonsSkillsInTheNewestVersionGroup {

        @ParameterizedTest(name = "The one where a move learned by {1} is shown as {0}, \"{1}\", level {2}")
        @CsvSource(nullValues = "none", value = {
                "tackle,       level-up, 1,    1",
                "swords-dance, machine,  0,    none",
                "skull-bash,   egg,      0,    none",
                "grass-pledge, tutor,    0,    none"
        })
        @DisplayName("The one where each move is shown with its name, how it is learned and, only when levelling up, the level")
        void showsHowEachMoveIsLearned(String move, String method, int levelGivenByPokeApi, Integer levelShown) {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur")
                    .withMoves(learns(move, method, levelGivenByPokeApi, "scarlet-violet"))));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].skills").asArray().hasSize(1);
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].name").isEqualTo(move);
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods").asArray().hasSize(1);
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods[0].method").isEqualTo(method);
            assertThat(response).bodyJson().hasPath("$.content[0].skills[0].methods[0].level");
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods[0].level")
                    .isEqualTo(levelShown);
        }

        @Test
        @DisplayName("The one where a move can be learned two ways (by level-up and by machine), so it is shown once with both methods")
        void moveLearnedTwoWaysIsShownOnceWithBothMethods() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur").withMoves(
                    learns("take-down", "level-up", 15, "scarlet-violet"),
                    learns("take-down", "machine", 0, "scarlet-violet"))));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[*].name").asArray()
                    .containsExactly("take-down");
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods[*].method").asArray()
                    .containsExactly("level-up", "machine");
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods[0].level").isEqualTo(15);
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods[1].level").isNull();
        }

        @Test
        @DisplayName("The one where a Pokémon does not appear in the newest version group. It is still listed, with no skills")
        void pokemonMissingFromTheNewestVersionGroupIsStillListedWithNoSkills() {
            pokeApi.serves(List.of(
                    pokemon(1, "bulbasaur").withMoves(learns("tackle", "level-up", 1, "scarlet-violet")),
                    pokemon(2, "ivysaur").withMoves(learns("tackle", "level-up", 1, "sword-shield"))));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(1, 2);
            assertThat(response).bodyJson().extractingPath("$.content[1].skills").asArray().isEmpty();
        }

        @Test
        @DisplayName("The one where a move the Pokémon could learn only in older games is left out")
        void moveLearnableOnlyInOlderGamesIsLeftOut() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur").withMoves(
                    learns("tackle", "level-up", 1, "scarlet-violet"),
                    learns("tackle", "level-up", 1, "red-blue"),
                    learns("cut", "machine", 0, "red-blue"))));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[*].name").asArray()
                    .containsExactly("tackle");
            assertThat(response).bodyJson().extractingPath("$.content[0].skills[0].methods").asArray().hasSize(1);
        }
    }

    @Nested
    @DisplayName("Must reject pagination requests that are not valid (400, with the standard error body)")
    class RejectsPaginationRequestsThatAreNotValid {

        @ParameterizedTest(name = "The one where page {0} of size {1} is rejected")
        @CsvSource({
                "-1,  20",
                "0,   0",
                "0,   101",
                "abc, 20"
        })
        @DisplayName("The one where a page or size that is not valid is rejected with the standard error body")
        void rejectsPaginationThatIsNotValid(String page, String size) {
            pokeApi.serves(filler(1, 30));

            var response = mvc.get().uri("/api/pokemon").param("page", page).param("size", size).exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(400);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Bad Request");
        }

        @Test
        @DisplayName("The one where page 0 of the maximum size 100 is accepted")
        void acceptsTheMaximumSize() {
            pokeApi.serves(filler(1, 30));

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "100").exchange();

            assertThat(response).hasStatusOk();
        }
    }

    @Nested
    @DisplayName("Must let anyone browse, signed in or not")
    class LetsAnyoneBrowse {

        @Test
        @DisplayName("The one where a visitor with no token gets page 0")
        void visitorWithNoTokenGetsPageZero() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur"), pokemon(2, "ivysaur")));

            var response = mvc.get().uri("/api/pokemon").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.page").isEqualTo(0);
            assertThat(response).bodyJson().extractingPath("$.content[*].name").asArray()
                    .containsExactly("bulbasaur", "ivysaur");
        }
    }

    @Nested
    @DisplayName("Should reuse PokeAPI responses for 24 hours instead of asking again")
    class ReusesPokeApiResponsesFor24Hours {

        @Test
        @DisplayName("The one where the same page is browsed twice within 24 hours, and PokeAPI is called only the first time")
        void samePageBrowsedTwiceWithin24HoursCallsPokeApiOnlyTheFirstTime() throws Exception {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur"), pokemon(2, "ivysaur")));
            var first = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();
            var callsAfterFirstBrowse = pokeApi.requestsReceived();

            clock.advance(Duration.ofHours(23));
            var second = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(callsAfterFirstBrowse).isPositive();
            assertThat(pokeApi.requestsReceived()).isEqualTo(callsAfterFirstBrowse);
            assertThat(second).hasStatusOk();
            assertThat(second).bodyJson().isEqualTo(first.getResponse().getContentAsString());
        }

        @Test
        @DisplayName("The one where more than 24 hours have passed, so PokeAPI is called again and the fresh data is shown")
        void after24HoursPokeApiIsCalledAgainAndFreshDataIsShown() {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur")
                    .withSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png")));
            mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();
            var callsAfterFirstBrowse = pokeApi.requestsReceived();

            pokeApi.serves(List.of(pokemon(1, "bulbasaur")
                    .withSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/home/1.png")));
            clock.advance(Duration.ofHours(24).plusMinutes(1));
            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(pokeApi.requestsReceived()).isGreaterThan(callsAfterFirstBrowse);
            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].sprite")
                    .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/home/1.png");
        }
    }

    @Nested
    @DisplayName("Must keep serving cached data while PokeAPI is unavailable, and otherwise fail clearly")
    class KeepsServingCachedDataWhilePokeApiIsUnavailable {

        @Test
        @DisplayName("The one where PokeAPI is down but the requested page is still cached, so the page is served as normal")
        void servesTheCachedPageWhilePokeApiIsDown() throws Exception {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur").withCategory("Seed Pokémon"), pokemon(2, "ivysaur")));
            var whileUp = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            pokeApi.goesDown();
            var whileDown = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(whileDown).hasStatusOk();
            assertThat(whileDown).bodyJson().isEqualTo(whileUp.getResponse().getContentAsString());
        }

        @Test
        @DisplayName("The one where PokeAPI is down and the page is not cached, so the reader gets a 503 with the standard error body and no upstream details")
        void failsClearlyWhenPokeApiIsDownAndThePageIsNotCached() throws Exception {
            pokeApi.serves(List.of(pokemon(1, "bulbasaur")));
            pokeApi.goesDown();

            var response = mvc.get().uri("/api/pokemon").param("page", "0").param("size", "20").exchange();

            assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(503);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Service Unavailable");
            assertThat(response.getResponse().getContentAsString())
                    .doesNotContain(pokeApi.baseUrl(), "localhost", "pokeapi-db-7", "Exception");
        }
    }

    private static StubPokemon pokemon(int id, String name) {
        return new StubPokemon(id, name, id, null, true, null, 0, List.of());
    }

    private static StubPokemon form(int id, String name, int speciesId) {
        return new StubPokemon(id, name, speciesId, null, true, null, 0, List.of());
    }

    private static StubMove learns(String move, String method, int level, String versionGroup) {
        return new StubMove(move, method, level, versionGroup);
    }

    /**
     * Base Pokémon whose names the example does not care about.
     */
    private static List<StubPokemon> filler(int fromId, int toId) {
        return IntStream.rangeClosed(fromId, toId).mapToObj(id -> pokemon(id, "pokemon-" + id)).toList();
    }

    /**
     * @param sprite     the default front sprite URL, or {@code null} when PokeAPI has none
     * @param hasDetails {@code false} when PokeAPI lists the Pokémon but answers 404 for {@code /pokemon/{id}}
     * @param category   English genus of the species whose id is this Pokémon's id, or {@code null} for none
     * @param moves      every way the Pokémon learns a move, in every version group
     */
    private record StubPokemon(int id, String name, int speciesId, String sprite, boolean hasDetails,
                               String category, int weight, List<StubMove> moves) {

        StubPokemon withSprite(String spriteUrl) {
            return new StubPokemon(id, name, speciesId, spriteUrl, hasDetails, category, weight, moves);
        }

        StubPokemon withoutDetails() {
            return new StubPokemon(id, name, speciesId, sprite, false, category, weight, moves);
        }

        StubPokemon withCategory(String englishGenus) {
            return new StubPokemon(id, name, speciesId, sprite, hasDetails, englishGenus, weight, moves);
        }

        StubPokemon withWeight(int hectograms) {
            return new StubPokemon(id, name, speciesId, sprite, hasDetails, category, hectograms, moves);
        }

        StubPokemon withMoves(StubMove... learnings) {
            return new StubPokemon(id, name, speciesId, sprite, hasDetails, category, weight, List.of(learnings));
        }
    }

    /**
     * One way a Pokémon learns a move in one version group; PokeAPI reports level 0 for methods other than level-up.
     */
    private record StubMove(String move, String method, int level, String versionGroup) {
    }

    /**
     * Minimal stand-in for PokeAPI v2: the paginated {@code /pokemon} list, {@code /pokemon/{id or name}},
     * {@code /pokemon-species/{id}} and the paginated {@code /version-group} list, served from the catalogue
     * the current test provides. The known version groups, oldest first, are always the same three.
     */
    private static final class StubPokeApi {

        private static final List<String> VERSION_GROUPS = List.of("red-blue", "sword-shield", "scarlet-violet");

        private final AtomicReference<List<StubPokemon>> catalogue = new AtomicReference<>(List.of());
        private final AtomicInteger requestsReceived = new AtomicInteger();
        private final AtomicBoolean down = new AtomicBoolean();
        private HttpServer server;

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v2/pokemon/", whileUp(this::pokemon));
            server.createContext("/api/v2/pokemon-species/", whileUp(this::species));
            server.createContext("/api/v2/version-group/", whileUp(this::versionGroups));
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
            down.set(false);
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

        private void pokemon(HttpExchange exchange) throws IOException {
            var key = lastPathSegment(exchange.getRequestURI(), "/api/v2/pokemon/");
            if (key.isEmpty()) {
                list(exchange);
                return;
            }
            var found = catalogue.get().stream()
                    .filter(p -> key.equals(String.valueOf(p.id())) || key.equals(p.name()))
                    .filter(StubPokemon::hasDetails)
                    .findFirst();
            if (found.isEmpty()) {
                respond(exchange, 404, "Not Found");
                return;
            }
            var p = found.get();
            respond(exchange, 200, """
                    {"id":%d,"name":"%s","weight":%d,"sprites":{"front_default":%s},\
                    "species":{"name":"%s","url":"%s/pokemon-species/%d/"},"moves":[%s]}"""
                    .formatted(p.id(), p.name(), p.weight(), p.sprite() == null ? "null" : "\"" + p.sprite() + "\"",
                            p.name(), baseUrl(), p.speciesId(), movesJson(p.moves())));
        }

        /**
         * PokeAPI lists each move once, with one version-group detail per way and game it is learned in.
         */
        private String movesJson(List<StubMove> moves) {
            var byMove = moves.stream()
                    .collect(Collectors.groupingBy(StubMove::move, LinkedHashMap::new, Collectors.toList()));
            return byMove.entrySet().stream()
                    .map(entry -> """
                            {"move":{"name":"%s","url":"%s/move/%s/"},"version_group_details":[%s]}"""
                            .formatted(entry.getKey(), baseUrl(), entry.getKey(), entry.getValue().stream()
                                    .map(m -> """
                                            {"level_learned_at":%d,\
                                            "move_learn_method":{"name":"%s","url":"%s/move-learn-method/%s/"},\
                                            "version_group":{"name":"%s","url":"%s/version-group/%d/"}}"""
                                            .formatted(m.level(), m.method(), baseUrl(), m.method(),
                                                    m.versionGroup(), baseUrl(),
                                                    VERSION_GROUPS.indexOf(m.versionGroup()) + 1))
                                    .collect(Collectors.joining(","))))
                    .collect(Collectors.joining(","));
        }

        private void list(HttpExchange exchange) throws IOException {
            var pokemon = catalogue.get().stream()
                    .map(p -> new StubResource(p.name(), p.id()))
                    .toList();
            respondWithPage(exchange, "pokemon", pokemon);
        }

        private void versionGroups(HttpExchange exchange) throws IOException {
            var versionGroups = IntStream.range(0, VERSION_GROUPS.size())
                    .mapToObj(i -> new StubResource(VERSION_GROUPS.get(i), i + 1))
                    .toList();
            respondWithPage(exchange, "version-group", versionGroups);
        }

        private void respondWithPage(HttpExchange exchange, String resource, List<StubResource> all)
                throws IOException {
            var query = queryParams(exchange.getRequestURI());
            int offset = Integer.parseInt(query.getOrDefault("offset", "0"));
            int limit = Integer.parseInt(query.getOrDefault("limit", "20"));
            var slice = all.subList(Math.min(offset, all.size()), Math.min(offset + limit, all.size()));
            var next = offset + limit < all.size()
                    ? "\"%s/%s/?offset=%d&limit=%d\"".formatted(baseUrl(), resource, offset + limit, limit)
                    : "null";
            var previous = offset > 0
                    ? "\"%s/%s/?offset=%d&limit=%d\"".formatted(baseUrl(), resource, Math.max(0, offset - limit), limit)
                    : "null";
            var results = slice.stream()
                    .map(r -> "{\"name\":\"%s\",\"url\":\"%s/%s/%d/\"}".formatted(r.name(), baseUrl(), resource, r.id()))
                    .collect(Collectors.joining(","));
            respond(exchange, 200, "{\"count\":%d,\"next\":%s,\"previous\":%s,\"results\":[%s]}"
                    .formatted(all.size(), next, previous, results));
        }

        private record StubResource(String name, int id) {
        }

        private void species(HttpExchange exchange) throws IOException {
            var key = lastPathSegment(exchange.getRequestURI(), "/api/v2/pokemon-species/");
            var englishGenus = catalogue.get().stream()
                    .filter(p -> key.equals(String.valueOf(p.id())))
                    .map(StubPokemon::category)
                    .filter(Objects::nonNull)
                    .map(genus -> ",{\"genus\":\"%s\",\"language\":{\"name\":\"en\",\"url\":\"%s/language/9/\"}}"
                            .formatted(genus, baseUrl()))
                    .findFirst()
                    .orElse("");
            respond(exchange, 200, """
                    {"id":%s,"genera":[{"genus":"ポケモン","language":{"name":"ja","url":"%s/language/11/"}}%s]}"""
                    .formatted(key, baseUrl(), englishGenus));
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