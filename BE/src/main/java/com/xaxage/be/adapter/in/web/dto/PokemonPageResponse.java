package com.xaxage.be.adapter.in.web.dto;

import com.xaxage.be.domain.model.PokemonPage;

import java.util.List;

public record PokemonPageResponse(List<PokemonResponse> content, boolean hasNext,
                                  int page, int size, int count, int totalPages) {

    public static PokemonPageResponse from(PokemonPage pokemonPage, int page, int size) {
        var content = pokemonPage.content().stream()
                .map(PokemonResponse::from)
                .toList();
        return new PokemonPageResponse(content, pokemonPage.hasNext(),
                page, size, pokemonPage.count(), pokemonPage.totalPages(size));
    }
}
