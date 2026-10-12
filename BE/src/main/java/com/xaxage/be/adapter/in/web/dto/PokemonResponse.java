package com.xaxage.be.adapter.in.web.dto;

import com.xaxage.be.domain.model.Pokemon;

public record PokemonResponse(int id, String name) {

    public static PokemonResponse from(Pokemon pokemon) {
        return new PokemonResponse(pokemon.id(), pokemon.name());
    }
}