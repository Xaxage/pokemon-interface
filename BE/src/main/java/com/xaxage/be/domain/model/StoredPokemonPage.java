package com.xaxage.be.domain.model;

import java.util.List;

/**
 * One page of the Pokémon stored in the local database.
 *
 * @param count how many Pokémon are stored in all
 */
public record StoredPokemonPage(List<StoredPokemon> content, int count) {

    public int totalPages(int size) {
        return Math.ceilDiv(count, size);
    }
}
