package com.xaxage.be.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A Pokémon stored in the local database")
class StoredPokemonTest {

    @Nested
    @DisplayName("Must copy a Pokémon from PokeAPI into the local database, with our own fields empty")
    class CopiesAPokemonFromPokeApi {

        @Test
        @DisplayName("The one where Bulbasaur is copied, so it keeps PokeAPI's id, name, category, weight and sprite "
                + "and has no localized name, no region and no tags")
        void copyKeepsPokeApisFieldsAndLeavesOurOwnEmpty() {
            var fromPokeApi = new Pokemon(1, "bulbasaur")
                    .withCategory("Seed Pokémon")
                    .withWeight(69)
                    .withSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png")
                    .withSkills(List.of(new Skill("tackle", List.of())));

            var stored = StoredPokemon.copyOf(fromPokeApi);

            assertThat(stored).isEqualTo(new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69,
                    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                    null, null, List.of()));
        }
    }

    @Nested
    @DisplayName("Must refresh PokeAPI's fields when a stored Pokémon is synchronised again, keeping our own fields")
    class RefreshesPokeApisFieldsKeepingOurOwn {

        @Test
        @DisplayName("The one where stored Bulbasaur is \"Bisasam\" from \"Kanto\" with tag \"starter\", so refreshing it "
                + "takes PokeAPI's new weight and sprite and keeps \"Bisasam\", \"Kanto\" and \"starter\"")
        void refreshTakesPokeApisFieldsAndKeepsOurOwn() {
            var stored = new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 60, "outdated-bulbasaur.png",
                    "Bisasam", "Kanto", List.of("starter"));
            var fromPokeApi = new Pokemon(1, "bulbasaur")
                    .withCategory("Seed Pokémon")
                    .withWeight(69)
                    .withSprite("bulbasaur.png");

            var refreshed = stored.refreshedFrom(fromPokeApi);

            assertThat(refreshed).isEqualTo(new StoredPokemon(1, "bulbasaur", "Seed Pokémon", 69, "bulbasaur.png",
                    "Bisasam", "Kanto", List.of("starter")));
        }
    }
}
