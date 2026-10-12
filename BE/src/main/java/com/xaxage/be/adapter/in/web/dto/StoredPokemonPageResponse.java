package com.xaxage.be.adapter.in.web.dto;

import com.xaxage.be.domain.model.StoredPokemonPage;

import java.util.List;

public record StoredPokemonPageResponse(List<StoredPokemonResponse> content, int page, int size, int count,
                                        int totalPages) {

    public static StoredPokemonPageResponse from(StoredPokemonPage storedPokemonPage, int page, int size) {
        var content = storedPokemonPage.content().stream()
                .map(StoredPokemonResponse::from)
                .toList();
        return new StoredPokemonPageResponse(content, page, size, storedPokemonPage.count(),
                storedPokemonPage.totalPages(size));
    }
}
