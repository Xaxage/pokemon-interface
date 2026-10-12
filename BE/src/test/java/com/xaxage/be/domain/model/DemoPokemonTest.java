package com.xaxage.be.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("The demo Pokémon the local database comes with")
class DemoPokemonTest {

    @Nested
    @DisplayName("Must come pre-populated with demo data")
    class ComesPrePopulatedWithDemoData {

        @Test
        @DisplayName("The one where the demo data is the first 10 Pokémon, Bulbasaur … Caterpie, in id order")
        void demoDataIsTheFirstTenPokemon() {
            assertThat(DemoPokemon.FIRST_TEN).extracting(StoredPokemon::id)
                    .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            assertThat(DemoPokemon.FIRST_TEN).extracting(StoredPokemon::name)
                    .containsExactly("bulbasaur", "ivysaur", "venusaur", "charmander", "charmeleon", "charizard",
                            "squirtle", "wartortle", "blastoise", "caterpie");
        }

        @ParameterizedTest(name = "The one where {1} is stored as a \"{2}\" weighing {3} hectograms, with its sprite and "
                + "our own fields empty")
        @CsvSource({
                "1, bulbasaur, Seed Pokémon, 69",
                "2, ivysaur, Seed Pokémon, 130",
                "3, venusaur, Seed Pokémon, 1000",
                "4, charmander, Lizard Pokémon, 85",
                "5, charmeleon, Flame Pokémon, 190",
                "6, charizard, Flame Pokémon, 905",
                "7, squirtle, Tiny Turtle Pokémon, 90",
                "8, wartortle, Turtle Pokémon, 225",
                "9, blastoise, Shellfish Pokémon, 855",
                "10, caterpie, Worm Pokémon, 29"})
        @DisplayName("The one where each demo Pokémon has PokeAPI's category, weight and sprite, and our own fields empty")
        void eachDemoPokemonHasPokeApisFields(int id, String name, String category, int weight) {
            assertThat(DemoPokemon.FIRST_TEN.get(id - 1)).isEqualTo(new StoredPokemon(id, name, category, weight,
                    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + id + ".png",
                    null, null, List.of()));
        }
    }
}
