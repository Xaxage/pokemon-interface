package com.xaxage.be.domain.model;

import java.util.List;

public record PokemonPage(List<Pokemon> content, boolean hasNext, int count) {

    public int totalPages(int size) {
        return Math.ceilDiv(count, size);
    }
}
