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
        return pokeApi.fetchPokemon(page * size, size);
    }
}