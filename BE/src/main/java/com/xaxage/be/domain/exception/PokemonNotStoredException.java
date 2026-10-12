package com.xaxage.be.domain.exception;

/**
 * No Pokémon with the id the reader chose is stored in the local database, whether or not PokeAPI knows one.
 */
public class PokemonNotStoredException extends RuntimeException {

    public PokemonNotStoredException(int id) {
        super("No Pokémon with the id " + id + " is stored");
    }
}
