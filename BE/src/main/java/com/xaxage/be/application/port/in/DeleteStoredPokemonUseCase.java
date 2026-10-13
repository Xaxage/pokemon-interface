package com.xaxage.be.application.port.in;

public interface DeleteStoredPokemonUseCase {

    /**
     * Removes the stored Pokémon with the id from the local database.
     */
    void delete(int id);
}
