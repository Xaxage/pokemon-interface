package com.xaxage.be.adapter.out.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xaxage.be.domain.model.Pokemon;

/**
 * PokeAPI's single {@code /pokemon/{id}} resource.
 */
public record PokemonDetailResponse(int id, String name, Sprites sprites) {

    public Pokemon toPokemon() {
        return new Pokemon(id, name, sprites == null ? null : sprites.frontDefault());
    }

    public record Sprites(@JsonProperty("front_default") String frontDefault) {
    }
}
