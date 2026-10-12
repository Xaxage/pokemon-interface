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
                    .willReturn(new PokemonPage(List.of(new Pokemon(1, "bulbasaur")), true));

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
                    .willReturn(new PokemonPage(List.of(), false));

            var response = mvc.get().uri("/api/pokemon").param("page", "2").param("size", "20").exchange();

            assertThat(response).hasStatusOk();
            assertThat(response).bodyJson().extractingPath("$.content").asArray().isEmpty();
            assertThat(response).bodyJson().extractingPath("$.hasNext").isEqualTo(false);
        }
    }
}