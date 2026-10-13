package com.xaxage.be.domain.model;

import java.util.List;

/**
 * Our local copy of a Pokémon: PokeAPI's fields, plus our own fields on top of them.
 *
 * @param category      English genus of the Pokémon's species, as PokeAPI gives it
 * @param weight        weight in hectograms, as PokeAPI gives it
 * @param sprite        link to the default front sprite, as PokeAPI gives it
 * @param localizedName our own name for the Pokémon, or {@code null} until one is given
 * @param region        our own region for the Pokémon, or {@code null} until one is given
 * @param tags          our own tags for the Pokémon, none until some are given
 */
public record StoredPokemon(int id, String name, String category, Integer weight, String sprite,
                            String localizedName, String region, List<String> tags) {

    /**
     * @return a new local copy of the Pokémon PokeAPI gave, with our own fields empty
     */
    public static StoredPokemon copyOf(Pokemon fromPokeApi) {
        return new StoredPokemon(fromPokeApi.id(), fromPokeApi.name(), fromPokeApi.category(), fromPokeApi.weight(),
                fromPokeApi.sprite(), null, null, List.of());
    }

    /**
     * @return this copy with PokeAPI's fields replaced by the ones PokeAPI gives now, and our own fields kept
     */
    public StoredPokemon refreshedFrom(Pokemon fromPokeApi) {
        return new StoredPokemon(id, fromPokeApi.name(), fromPokeApi.category(), fromPokeApi.weight(),
                fromPokeApi.sprite(), localizedName, region, tags);
    }

    /**
     * @param tags our own tags, or {@code null} for none
     * @return this copy with our own fields replaced by the ones given, and PokeAPI's fields kept
     */
    public StoredPokemon editedWith(String localizedName, String region, List<String> tags) {
        return new StoredPokemon(id, name, category, weight, sprite, localizedName, region,
                tags == null ? List.of() : List.copyOf(tags));
    }
}
