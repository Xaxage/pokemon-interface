package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.ViewPokemonUseCase;
import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.domain.exception.PokemonNotFoundException;
import com.xaxage.be.domain.model.DetailedPokemon;
import org.springframework.stereotype.Service;

@Service
public class ViewPokemonService implements ViewPokemonUseCase {

    private final PokeApiPort pokeApi;

    public ViewPokemonService(PokeApiPort pokeApi) {
        this.pokeApi = pokeApi;
    }

    @Override
    public DetailedPokemon view(int id) {
        return pokeApi.fetchDetailedPokemon(id).orElseThrow(() -> new PokemonNotFoundException(id));
    }
}
