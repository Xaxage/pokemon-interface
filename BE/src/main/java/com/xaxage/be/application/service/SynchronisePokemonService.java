package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.SynchronisePokemonUseCase;
import com.xaxage.be.application.port.out.PokeApiPort;
import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.exception.PokemonNotFoundException;
import com.xaxage.be.domain.model.StoredPokemon;
import org.springframework.stereotype.Service;

@Service
public class SynchronisePokemonService implements SynchronisePokemonUseCase {

    private final PokeApiPort pokeApi;
    private final StoredPokemonRepositoryPort storedPokemon;

    public SynchronisePokemonService(PokeApiPort pokeApi, StoredPokemonRepositoryPort storedPokemon) {
        this.pokeApi = pokeApi;
        this.storedPokemon = storedPokemon;
    }

    @Override
    public StoredPokemon synchronise(int id) {
        var fromPokeApi = pokeApi.fetchPokemonDetails(id).orElseThrow(() -> new PokemonNotFoundException(id));
        return storedPokemon.save(StoredPokemon.copyOf(fromPokeApi));
    }
}
