package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.DeleteStoredPokemonUseCase;
import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.exception.PokemonNotStoredException;
import org.springframework.stereotype.Service;

@Service
public class DeleteStoredPokemonService implements DeleteStoredPokemonUseCase {

    private final StoredPokemonRepositoryPort storedPokemon;

    public DeleteStoredPokemonService(StoredPokemonRepositoryPort storedPokemon) {
        this.storedPokemon = storedPokemon;
    }

    @Override
    public void delete(int id) {
        if (storedPokemon.findById(id).isEmpty()) {
            throw new PokemonNotStoredException(id);
        }
        storedPokemon.delete(id);
    }
}
