package com.xaxage.be.application.port.out;

import com.xaxage.be.domain.model.PokemonPage;

public interface PokeApiPort {

    PokemonPage fetchPokemon(int offset, int limit);
}