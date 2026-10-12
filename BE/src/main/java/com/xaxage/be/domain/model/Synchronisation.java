package com.xaxage.be.domain.model;

/**
 * What synchronising a Pokémon with PokeAPI did to the local database.
 */
public sealed interface Synchronisation {

    /**
     * @return the local copy, as it is after synchronising
     */
    StoredPokemon pokemon();

    /**
     * The Pokémon was not stored yet, so a new local copy was made.
     */
    record Copied(StoredPokemon pokemon) implements Synchronisation {
    }

    /**
     * The Pokémon was already stored, so its PokeAPI fields were refreshed and our own fields kept.
     */
    record Refreshed(StoredPokemon pokemon) implements Synchronisation {
    }
}
