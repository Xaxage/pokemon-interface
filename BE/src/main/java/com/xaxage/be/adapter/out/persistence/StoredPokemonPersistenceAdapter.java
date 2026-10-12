package com.xaxage.be.adapter.out.persistence;

import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.model.StoredPokemon;
import org.springframework.stereotype.Component;

@Component
class StoredPokemonPersistenceAdapter implements StoredPokemonRepositoryPort {

    private final StoredPokemonJpaRepository repository;

    StoredPokemonPersistenceAdapter(StoredPokemonJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public StoredPokemon save(StoredPokemon pokemon) {
        return repository.save(StoredPokemonEntity.from(pokemon)).toStoredPokemon();
    }
}
