package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.EditStoredPokemonUseCase;
import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.exception.PokemonNotStoredException;
import com.xaxage.be.domain.model.StoredPokemon;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EditStoredPokemonService implements EditStoredPokemonUseCase {

    private final StoredPokemonRepositoryPort storedPokemon;

    public EditStoredPokemonService(StoredPokemonRepositoryPort storedPokemon) {
        this.storedPokemon = storedPokemon;
    }

    @Override
    public StoredPokemon edit(int id, String localizedName, String region, List<String> tags) {
        var stored = storedPokemon.findById(id).orElseThrow(() -> new PokemonNotStoredException(id));
        return storedPokemon.save(stored.editedWith(localizedName, region, tags));
    }
}
