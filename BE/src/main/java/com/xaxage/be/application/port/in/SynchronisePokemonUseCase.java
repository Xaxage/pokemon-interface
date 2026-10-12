package com.xaxage.be.application.port.in;

import com.xaxage.be.domain.model.StoredPokemon;

public interface SynchronisePokemonUseCase {

    /**
     * Copies the Pokémon with the id from PokeAPI into the local database.
     *
     * @return the local copy
     */
    StoredPokemon synchronise(int id);
}
