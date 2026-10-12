package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.ViewStoredPokemonUseCase;
import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.exception.PokemonNotStoredException;
import com.xaxage.be.domain.model.StoredPokemon;
import org.springframework.stereotype.Service;

@Service
public class ViewStoredPokemonService implements ViewStoredPokemonUseCase {

    private final StoredPokemonRepositoryPort storedPokemon;

    public ViewStoredPokemonService(StoredPokemonRepositoryPort storedPokemon) {
        this.storedPokemon = storedPokemon;
    }

    @Override
    public StoredPokemon view(int id) {
        return storedPokemon.findById(id).orElseThrow(() -> new PokemonNotStoredException(id));
    }
}
