package com.xaxage.be.application.port.out;

import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;

import java.util.Optional;

public interface PokeApiPort {

    PokemonPage fetchPokemon(int offset, int limit);

    /**
     * @return the Pokémon's details, or empty when PokeAPI has none for it
     */
    Optional<Pokemon> fetchPokemonDetails(int id);

    /**
     * @return the name of the newest version group (games) known to PokeAPI
     */
    String fetchNewestVersionGroup();
}
