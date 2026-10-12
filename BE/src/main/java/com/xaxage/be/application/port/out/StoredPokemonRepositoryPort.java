package com.xaxage.be.application.port.out;

import com.xaxage.be.domain.model.StoredPokemon;

public interface StoredPokemonRepositoryPort {

    /**
     * Stores the Pokémon, replacing the one with the same id if there is one.
     *
     * @return the Pokémon as stored
     */
    StoredPokemon save(StoredPokemon pokemon);
}
