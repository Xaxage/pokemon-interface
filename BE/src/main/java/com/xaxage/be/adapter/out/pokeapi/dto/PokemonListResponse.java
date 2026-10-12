package com.xaxage.be.adapter.out.pokeapi.dto;

import com.xaxage.be.domain.model.Pokemon;
import com.xaxage.be.domain.model.PokemonPage;

import java.util.List;

/**
 * PokeAPI's paginated {@code /pokemon} list.
 */
public record PokemonListResponse(int count, String next, List<NamedResource> results) {

    public PokemonPage toPokemonPage() {
        var content = results.stream()
                .map(resource -> new Pokemon(resource.id(), resource.name()))
                .toList();
        return new PokemonPage(content, next != null, count);
    }
}
