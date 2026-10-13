package com.xaxage.be.adapter.out.persistence;

import com.xaxage.be.application.port.out.StoredPokemonRepositoryPort;
import com.xaxage.be.domain.model.StoredPokemon;
import com.xaxage.be.domain.model.StoredPokemonPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class StoredPokemonPersistenceAdapter implements StoredPokemonRepositoryPort {

    private final StoredPokemonJpaRepository repository;

    StoredPokemonPersistenceAdapter(StoredPokemonJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<StoredPokemon> findById(int id) {
        return repository.findById(id).map(StoredPokemonEntity::toStoredPokemon);
    }

    @Override
    public StoredPokemon save(StoredPokemon pokemon) {
        return repository.save(StoredPokemonEntity.from(pokemon)).toStoredPokemon();
    }

    @Override
    public void delete(int id) {
        repository.deleteById(id);
    }

    @Override
    public StoredPokemonPage findPage(int page, int size) {
        var found = repository.findAll(PageRequest.of(page, size, Sort.by("id")));
        return new StoredPokemonPage(found.map(StoredPokemonEntity::toStoredPokemon).getContent(),
                Math.toIntExact(found.getTotalElements()));
    }

    @Override
    public boolean isEmpty() {
        return repository.count() == 0;
    }
}
