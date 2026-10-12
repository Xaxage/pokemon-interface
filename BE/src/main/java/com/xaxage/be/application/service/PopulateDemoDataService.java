package com.xaxage.be.application.service;

import com.xaxage.be.application.port.in.PopulateDemoDataUseCase;
import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.model.DemoPokemon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PopulateDemoDataService implements PopulateDemoDataUseCase {

    private final StoredPokemonRepositoryPort storedPokemon;

    public PopulateDemoDataService(StoredPokemonRepositoryPort storedPokemon) {
        this.storedPokemon = storedPokemon;
    }

    @Override
    @Transactional
    public void populate() {
        if (storedPokemon.isEmpty()) {
            DemoPokemon.FIRST_TEN.forEach(storedPokemon::save);
        }
    }
}
