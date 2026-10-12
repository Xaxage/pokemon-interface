package com.xaxage.be.application.port.in;

import com.xaxage.be.domain.model.Synchronisation;

public interface SynchronisePokemonUseCase {

    /**
     * Copies the Pokémon with the id from PokeAPI into the local database, or refreshes the copy already there.
     */
    Synchronisation synchronise(int id);
}
