package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.BrowseStoredPokemonUseCase;
import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.model.StoredPokemonPage;
import org.springframework.stereotype.Service;

@Service
public class BrowseStoredPokemonService implements BrowseStoredPokemonUseCase {

    private final StoredPokemonRepositoryPort storedPokemon;

    public BrowseStoredPokemonService(StoredPokemonRepositoryPort storedPokemon) {
        this.storedPokemon = storedPokemon;
    }

    @Override
    public StoredPokemonPage browse(int page, int size) {
        return storedPokemon.findPage(page, size);
    }
}
