package com.xaxage.be.adapter.in.web;

import com.xaxage.be.application.port.in.BrowsePokemonUseCase;
import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(PokemonController.class)
@DisplayName("Pokémon browse endpoint")
class PokemonControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private BrowsePokemonUseCase browsePokemon;

    @Nested
    @DisplayName("Browsing a page of Pokémon")
    class BrowsingAPage {

        @Test
        @DisplayName("The one where no page or size is given, so page 0 of 20 is asked for and returned as-is")
        void defaultsToFirstPageOfTwenty() {
            given(browsePokemon.browse(0, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(1, "bulbasaur")), true, 1350));

            var response = mvc.get().uri("/api/pokemon").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].id").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.content[0].name").isEqualTo("bulbasaur");
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(true);
        }

        @Test
        @DisplayName("The one where a page past the last page comes back with an empty list of entries and no next page")
        void pagePastTheLastHasEmptyContent() {
            given(browsePokemon.browse(2, 20))
                    .willReturn(new PokemonPage(List.of(), false, 1350));

            var response = mvc.get().uri("/api/pokemon").param("page", "2").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content").asArray().isEmpty();
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("Must tell the reader where they are in the full list")
    class WhereTheReaderIsInTheFullList {

        @Test
        @DisplayName("The one where page 1 of size 20 out of 45 Pokémon reports page 1, size 20, a count of 45 and 3 pages")
        void reportsPagePositionAndTotals() {
            given(browsePokemon.browse(1, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(21, "spearow")), true, 45));

            var response = mvc.get().uri("/api/pokemon").param("page", "1").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.page").isEqualTo(1);
            assertThat(response).bodyJson().extractingPath("$.size").isEqualTo(20);
            assertThat(response).bodyJson().extractingPath("$.count").isEqualTo(45);
            assertThat(response).bodyJson().extractingPath("$.totalPages").isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Must show each Pokémon's default front sprite")
    class ShowsEachPokemonsDefaultFrontSprite {

        @Test
        @DisplayName("The one where Bulbasaur comes back with the link to its default front battle sprite")
        void showsTheSpriteLink() {
            given(browsePokemon.browse(0, 20))
                    .willReturn(new PokemonPage(List.of(new Pokemon(1, "bulbasaur",
                            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png")), false, 1));

            var response = mvc.get().uri("/api/pokemon").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content[0].sprite")
                    .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png");
        }
    }
}