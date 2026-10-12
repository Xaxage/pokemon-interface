package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.in.BrowseStoredPokemonUseCase;
import com.xaxage.be.application.port.in.SynchronisePokemonUseCase;
import com.xaxage.be.domain.model.StoredPokemon;
import com.xaxage.be.domain.model.StoredPokemonPage;
import com.xaxage.be.domain.model.Synchronisation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@WebMvcTest(StoredPokemonController.class)
@DisplayName("Stored Pokémon endpoints")
class StoredPokemonControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SynchronisePokemonUseCase synchronisePokemon;

    @MockitoBean
    private BrowseStoredPokemonUseCase browseStoredPokemon;

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

            var response = mvc.post().uri("/api/stored-pokemon/{id}/sync", 1).exchange();

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

            var response = mvc.post().uri("/api/stored-pokemon/{id}/sync", 1).exchange();

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
}
