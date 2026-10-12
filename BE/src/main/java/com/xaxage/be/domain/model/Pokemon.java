package com.xaxage.be.domain.model;

/**
 * @param sprite link to the default front sprite, or {@code null} when the Pokémon has none
 */
public record Pokemon(int id, String name, String sprite) {

    public Pokemon(int id, String name) {
        this(id, name, null);
    }
}
