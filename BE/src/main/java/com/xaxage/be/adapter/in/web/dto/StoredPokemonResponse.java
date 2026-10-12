package com.xaxage.be.adapter.in.web.dto;

import com.xaxage.be.domain.model.StoredPokemon;

import java.util.List;

public record StoredPokemonResponse(int id, String name, String category, Integer weight, String sprite,
                                    String localizedName, String region, List<String> tags) {

    public static StoredPokemonResponse from(StoredPokemon pokemon) {
        return new StoredPokemonResponse(pokemon.id(), pokemon.name(), pokemon.category(), pokemon.weight(),
                pokemon.sprite(), pokemon.localizedName(), pokemon.region(), pokemon.tags());
    }
}
