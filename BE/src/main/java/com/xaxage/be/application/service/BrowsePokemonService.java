package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.BrowsePokemonUseCase;
import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.domain.model.PokemonPage;
import org.springframework.stereotype.Service;

@Service
public class BrowsePokemonService implements BrowsePokemonUseCase {

    private final PokeApiPort pokeApi;

    public BrowsePokemonService(PokeApiPort pokeApi) {
        this.pokeApi = pokeApi;
    }

    @Override
    public PokemonPage browse(int page, int size) {
        var listed = pokeApi.fetchPokemon(page * size, size);
        var newestVersionGroup = pokeApi.fetchNewestVersionGroup();
        var detailed = listed.content().stream()
                .map(pokemon -> pokeApi.fetchPokemonDetails(pokemon.id()).orElse(pokemon))
                .map(pokemon -> pokemon.learnableIn(newestVersionGroup))
                .toList();
        return new PokemonPage(detailed, listed.hasNext(), listed.count());
    }
}