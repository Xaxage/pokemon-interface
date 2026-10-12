package com.xaxage.be.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface StoredPokemonJpaRepository extends JpaRepository<StoredPokemonEntity, Integer> {
}
