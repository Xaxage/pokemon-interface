package com.xaxage.be.application.port.out;

import com.xaxage.be.domain.model.StoredPokemon;

import java.util.Optional;

public interface StoredPokemonRepositoryPort {

    /**
     * @return the stored Pokémon with the id, or empty when none is stored
     */
    Optional<StoredPokemon> findById(int id);

    /**
     * Stores the Pokémon, replacing the one with the same id if there is one.
     *
     * @return the Pokémon as stored
     */
    StoredPokemon save(StoredPokemon pokemon);
}
