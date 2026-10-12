package com.xaxage.be.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A Pokémon in its detailed view")
class DetailedPokemonTest {

    @Nested
    @DisplayName("Must show the Pokémon's official artwork as its image")
    class Image {

        @Test
        @DisplayName("The one where Pikachu Rock Star has no official artwork, so its default front sprite is its image")
        void fallsBackToTheDefaultFrontSprite() {
            var rockStar = new DetailedPokemon(10080, "pikachu-rock-star").withImage(null,
                    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/10080.png");

            assertThat(rockStar.image())
                    .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/10080.png");
        }
    }

    @Nested
    @DisplayName("Must show an English description from the Pokémon's species, taken from the newest game that has one")
    class Description {

        @Test
        @DisplayName("The one where Bulbasaur's Red description has line and page breaks, so they are replaced by spaces")
        void replacesLineAndPageBreaksBySpaces() {
            var bulbasaur = new DetailedPokemon(1, "bulbasaur").withDescription(
                    "A strange seed was\nplanted on its\nback at birth.\fThe plant sprouts\nand grows with\nthis POKéMON.");

            assertThat(bulbasaur.description()).isEqualTo(
                    "A strange seed was planted on its back at birth. The plant sprouts and grows with this POKéMON.");
        }

        @Test
        @DisplayName("The one where the species has no English description, so the description is empty")
        void staysEmptyWithoutADescription() {
            var bulbasaur = new DetailedPokemon(1, "bulbasaur").withDescription(null);

            assertThat(bulbasaur.description()).isNull();
        }
    }
}
