package com.xaxage.be.domain.exception;

/**
 * No Pokémon has the id the reader chose.
 */
public class PokemonNotFoundException extends RuntimeException {

    public PokemonNotFoundException(int id) {
        super("No Pokémon has the id " + id);
    }
}
