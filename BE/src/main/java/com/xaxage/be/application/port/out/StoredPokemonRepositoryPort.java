package com.xaxage.be.application.port.out;

import com.xaxage.be.domain.model.StoredPokemon;
import com.xaxage.be.domain.model.StoredPokemonPage;

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

    /**
     * Removes the stored Pokémon with the id, together with our own fields.
     */
    void delete(int id);

    /**
     * @return the given page of stored Pokémon, ordered by id, with how many are stored in all
     */
    StoredPokemonPage findPage(int page, int size);

    /**
     * @return whether no Pokémon at all is stored
     */
    boolean isEmpty();
}
