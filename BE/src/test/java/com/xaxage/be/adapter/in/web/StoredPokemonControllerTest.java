package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.in.BrowseStoredPokemonUseCase;
import com.xaxage.be.application.port.in.EditStoredPokemonUseCase;
import com.xaxage.be.application.port.in.SynchronisePokemonUseCase;
import com.xaxage.be.application.port.in.ViewStoredPokemonUseCase;
import com.xaxage.be.domain.exception.PokemonNotStoredException;
import com.xaxage.be.domain.model.StoredPokemon;
import com.xaxage.be.domain.model.StoredPokemonPage;
import com.xaxage.be.domain.model.Synchronisation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(StoredPokemonController.class)
@Import(SecurityConfig.class)
@DisplayName("Stored Pokémon endpoints")
class StoredPokemonControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SynchronisePokemonUseCase synchronisePokemon;

    @MockitoBean
    private BrowseStoredPokemonUseCase browseStoredPokemon;

    @MockitoBean
    private ViewStoredPokemonUseCase viewStoredPokemon;

    @MockitoBean
    private EditStoredPokemonUseCase editStoredPokemon;

    @Nested
    @DisplayName("Must copy a Pokémon from PokeAPI into the local database, with our own fields empty")
    class CopiesAPokemonFromPokeApi {

        @Test
        @DisplayName("The one where Bulbasaur is synchronised for the first time, so its local copy comes back with 201")
        void firstSynchronisationAnswers201WithTheLocalCopy() {
            given(synchronisePokemon.synchronise(1)).willReturn(new Synchronisation.Copied(new StoredPokemon(1,
                    "bulbasaur", "Seed Pokémon", 69,
                    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                    null, null, List.of())));

            var response = mvc.post().uri("/api/stored-pokemon/{id}/sync", 1).with(jwt()).exchange();

            assertThat(response).hasStatus(HttpStatus.CREATED);
            assertThat(response).bodyJson().extractingPath("$.id").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.category").isEqualTo("Seed Pokémon");
            assertThat(response).bodyJson().extractingPath("$.weight").isEqualTo(69);
            assertThat(response).bodyJson().extractingPath("$.sprite")
                    .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
            assertThat(response).bodyJson().hasPath("$.localizedName");
            assertThat(response).bodyJson().extractingPath("$.localizedName").isNull();
            assertThat(response).bodyJson().hasPath("$.region");
            assertThat(response).bodyJson().extractingPath("$.region").isNull();
            assertThat(response).bodyJson().extractingPath("$.tags").asArray().isEmpty();
        }
    }

    @Nested
    @DisplayName("Must refresh PokeAPI's fields when a stored Pokémon is synchronised again, keeping our own fields")
    class RefreshesPokeApisFieldsKeepingOurOwn {

        @Test
        @DisplayName("The one where stored Bulbasaur is synchronised again, so its refreshed copy comes back with 200")
        void resynchronisationAnswers200WithTheRefreshedCopy() {
            given(synchronisePokemon.synchronise(1)).willReturn(new Synchronisation.Refreshed(new StoredPokemon(1,
                    "bulbasaur", "Seed Pokémon", 69, "bulbasaur.png", "Bisasam", "Kanto", List.of("starter"))));

            var response = mvc.post().uri("/api/stored-pokemon/{id}/sync", 1).with(jwt()).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.localizedName").isEqualTo("Bisasam");
            assertThat(response).bodyJson().extractingPath("$.region").isEqualTo("Kanto");
            assertThat(response).bodyJson().extractingPath("$.tags").asArray().containsExactly("starter");
        }
    }

    @Nested
    @DisplayName("Must list stored Pokémon one page at a time, ordered by id")
    class ListsStoredPokemonOnePageAtATime {

        @Test
        @DisplayName("The one where page 1 of size 2 is asked for out of 5 stored, so it comes back with its position "
                + "and the totals")
        void answersThePageWithItsPositionAndTheTotals() {
            given(browseStoredPokemon.browse(1, 2)).willReturn(new StoredPokemonPage(List.of(
                    new StoredPokemon(7, "squirtle", "Tiny Turtle Pokémon", 90, "squirtle.png", null, null, List.of()),
                    new StoredPokemon(8, "wartortle", "Turtle Pokémon", 225, "wartortle.png", null, null, List.of())),
                    5));

            var response = mvc.get().uri("/api/stored-pokemon?page=1&size=2").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(7, 8);
            assertThat(response).bodyJson().extractingPath("$.content[0].name").isEqualTo("squirtle");
            assertThat(response).bodyJson().extractingPath("$.page").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.size").isEqualTo(2);
            assertThat(response).bodyJson().extractingPath("$.count").isEqualTo(5);
            assertThat(response).bodyJson().extractingPath("$.totalPages").isEqualTo(3);
        }

        @ParameterizedTest(name = "The one where page {0} of size {1} is asked for, so it is rejected")
        @CsvSource({"-1, 20", "0, 0", "0, 101"})
        @DisplayName("The one where the page is negative or the size is not between 1 and 100, so the reader gets a 400 "
                + "with the standard error body")
        void rejectsPaginationThatIsNotValid(int page, int size) {
            var response = mvc.get().uri("/api/stored-pokemon?page={page}&size={size}", page, size).exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(400);
            then(browseStoredPokemon).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Must show one stored Pokémon by its id")
    class ShowsOneStoredPokemonByItsId {

        @Test
        @DisplayName("The one where stored Bulbasaur is asked for, so it comes back with PokeAPI's fields and our own")
        void answersTheStoredPokemon() {
            given(viewStoredPokemon.view(1)).willReturn(new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69,
                    "bulbasaur.png", "Bisasam", "Kanto", List.of("starter", "grass")));

            var response = mvc.get().uri("/api/stored-pokemon/{id}", 1).exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.weight").isEqualTo(69);
            assertThat(response).bodyJson().extractingPath("$.localizedName").isEqualTo("Bisasam");
            assertThat(response).bodyJson().extractingPath("$.tags").asArray().containsExactly("starter", "grass");
        }

        @Test
        @DisplayName("The one where id 25 is not stored, so the reader gets a 404 with the standard error body")
        void pokemonNotStoredAnswers404() {
            given(viewStoredPokemon.view(25)).willThrow(new PokemonNotStoredException(25));

            var response = mvc.get().uri("/api/stored-pokemon/{id}", 25).exchange();

            assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(404);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Not Found");
            assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("No Pokémon with the id 25 is stored");
        }
    }

    @Nested
    @DisplayName("Must let only signed-in users synchronise; anyone can read the local list and details")
    class LetsOnlySignedInUsersSynchronise {

        @Test
        @DisplayName("The one where a visitor with no token synchronises, so they get a 401 with the standard error body")
        void visitorWithNoTokenGets401() {
            var response = mvc.post().uri("/api/stored-pokemon/{id}/sync", 1).exchange();

            assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(401);
            assertThat(response).bodyJson().extractingPath("$.title").isEqualTo("Unauthorized");
            then(synchronisePokemon).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Must replace our own fields of a stored Pokémon with the ones sent")
    class ReplacesOurOwnFields {

        @Test
        @DisplayName("The one where Bulbasaur is updated with \"Bisasam\", \"Kanto\" and tags \"starter\" and \"grass\", "
                + "so the edited Pokémon comes back with 200")
        void answersTheEditedPokemon() {
            given(editStoredPokemon.edit(1, "Bisasam", "Kanto", List.of("starter", "grass")))
                    .willReturn(new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69, "bulbasaur.png",
                            "Bisasam", "Kanto", List.of("starter", "grass")));

            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1).with(jwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"localizedName":"Bisasam","region":"Kanto","tags":["starter","grass"]}""")
                    .exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.id").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.weight").isEqualTo(69);
            assertThat(response).bodyJson().extractingPath("$.localizedName").isEqualTo("Bisasam");
            assertThat(response).bodyJson().extractingPath("$.region").isEqualTo("Kanto");
            assertThat(response).bodyJson().extractingPath("$.tags").asArray().containsExactly("starter", "grass");
        }
    }

    @Nested
    @DisplayName("Must reject updates with invalid values (400, with the standard error body listing each invalid field)")
    class RejectsUpdatesWithInvalidValues {

        static Stream<Arguments> rejectedValues() {
            return Stream.of(
                    Arguments.of("a blank localized name", """
                            {"localizedName":"  "}""", "localizedName"),
                    Arguments.of("a localized name over 50 characters", """
                            {"localizedName":"%s"}""".formatted("a".repeat(51)), "localizedName"),
                    Arguments.of("a blank region", """
                            {"region":""}""", "region"),
                    Arguments.of("a region over 50 characters", """
                            {"region":"%s"}""".formatted("k".repeat(51)), "region"),
                    Arguments.of("more than 10 tags", """
                            {"tags":["t1","t2","t3","t4","t5","t6","t7","t8","t9","t10","t11"]}""", "tags"),
                    Arguments.of("a blank tag", """
                            {"tags":["starter"," "]}""", "tags[1]"),
                    Arguments.of("a tag over 30 characters", """
                            {"tags":["%s"]}""".formatted("t".repeat(31)), "tags[0]"),
                    Arguments.of("a duplicate tag", """
                            {"tags":["starter","grass","starter"]}""", "tags"));
        }

        @ParameterizedTest(name = "The one where the update sends {0}, so it is rejected naming the field")
        @MethodSource("rejectedValues")
        @DisplayName("The one where the update sends a value that is not valid, so the reader gets a 400 with the "
                + "standard error body naming the field")
        void rejectsTheInvalidField(String what, String body, String invalidField) {
            var response = mvc.put().uri("/api/stored-pokemon/{id}", 1).with(jwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .exchange();

            assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(response).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(response).bodyJson().extractingPath("$.status").isEqualTo(400);
            assertThat(response).bodyJson().extractingPath("$.invalidFields[*].field").asArray()
                    .containsExactly(invalidField);
            then(editStoredPokemon).shouldHaveNoInteractions();
        }
    }
}
