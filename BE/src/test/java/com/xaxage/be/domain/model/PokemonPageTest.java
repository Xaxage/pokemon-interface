package com.xaxage.be.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A page of Pokémon")
class PokemonPageTest {

    @Nested
    @DisplayName("Must tell the reader where they are in the full list")
    class WhereTheReaderIsInTheFullList {

        @Test
        @DisplayName("The one where 45 Pokémon in pages of 20 fill 3 pages, the last one only partly")
        void partlyFilledLastPageStillCountsAsAPage() {
            var page = new PokemonPage(List.of(new Pokemon(1, "bulbasaur")), true, 45);

            assertThat(page.totalPages(20)).isEqualTo(3);
        }

        @Test
        @DisplayName("The one where 40 Pokémon in pages of 20 fill exactly 2 pages, with no extra empty page")
        void fullLastPageAddsNoExtraPage() {
            var page = new PokemonPage(List.of(new Pokemon(1, "bulbasaur")), true, 40);

            assertThat(page.totalPages(20)).isEqualTo(2);
        }
    }
}
