package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.in.SynchronisePokemonUseCase;
import com.xaxage.be.domain.model.StoredPokemon;
import com.xaxage.be.domain.model.Synchronisation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(StoredPokemonController.class)
@DisplayName("Stored Pokémon endpoints")
class StoredPokemonControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SynchronisePokemonUseCase synchronisePokemon;

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
}
