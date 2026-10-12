package com.xaxage.be.adapter.in.web.dto;

import com.xaxage.be.domain.model.Pokemon;

import java.util.List;

public record PokemonResponse(int id, String name, String sprite, String category, Integer weight,
                              List<SkillResponse> skills) {

    public static PokemonResponse from(Pokemon pokemon) {
        return new PokemonResponse(pokemon.id(), pokemon.name(), pokemon.sprite(), pokemon.category(),
                pokemon.weight(), pokemon.skills().stream().map(SkillResponse::from).toList());
    }
}
