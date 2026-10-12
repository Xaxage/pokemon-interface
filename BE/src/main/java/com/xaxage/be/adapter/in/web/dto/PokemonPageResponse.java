package com.xaxage.be.adapter.in.web.dto;

import com.xaxage.be.domain.model.PokemonPage;

import java.util.List;

public record PokemonPageResponse(List<PokemonResponse> content, boolean hasNext) {

    public static PokemonPageResponse from(PokemonPage page) {
        var content = page.content().stream()
                .map(PokemonResponse::from)
                .toList();
        return new PokemonPageResponse(content, page.hasNext());
    }
}