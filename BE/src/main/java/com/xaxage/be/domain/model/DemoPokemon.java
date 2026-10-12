package com.xaxage.be.domain.model;

import java.util.List;

/**
 * The Pokémon the local database comes with, so there is something to browse before anyone synchronises.
 */
public final class DemoPokemon {

    private static final String SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/";

    /**
     * The first 10 Pokémon, Bulbasaur … Caterpie, in id order.
     */
    public static final List<StoredPokemon> FIRST_TEN = List.of(
            demo(1, "bulbasaur", "Seed Pokémon", 69),
            demo(2, "ivysaur", "Seed Pokémon", 130),
            demo(3, "venusaur", "Seed Pokémon", 1000),
            demo(4, "charmander", "Lizard Pokémon", 85),
            demo(5, "charmeleon", "Flame Pokémon", 190),
            demo(6, "charizard", "Flame Pokémon", 905),
            demo(7, "squirtle", "Tiny Turtle Pokémon", 90),
            demo(8, "wartortle", "Turtle Pokémon", 225),
            demo(9, "blastoise", "Shellfish Pokémon", 855),
            demo(10, "caterpie", "Worm Pokémon", 29));

    private DemoPokemon() {
    }

    /**
     * @param category English genus of the species, as PokeAPI gives it
     * @param weight   in hectograms, as PokeAPI gives it
     */
    private static StoredPokemon demo(int id, String name, String category, int weight) {
        return new StoredPokemon(id, name, category, weight, SPRITES + id + ".png", null, null, List.of());
    }
}
