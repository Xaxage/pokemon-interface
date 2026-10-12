package com.xaxage.be.adapter.out.persistence;

import com.xaxage.be.domain.model.StoredPokemon;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stored_pokemon")
class StoredPokemonEntity {

    @Id
    private int id;

    private String name;

    private String category;

    private Integer weight;

    private String sprite;

    @Column(name = "localized_name")
    private String localizedName;

    private String region;

    @ElementCollection
    @CollectionTable(name = "stored_pokemon_tag", joinColumns = @JoinColumn(name = "pokemon_id"))
    @OrderColumn(name = "position")
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    protected StoredPokemonEntity() {
    }

    static StoredPokemonEntity from(StoredPokemon pokemon) {
        var entity = new StoredPokemonEntity();
        entity.id = pokemon.id();
        entity.name = pokemon.name();
        entity.category = pokemon.category();
        entity.weight = pokemon.weight();
        entity.sprite = pokemon.sprite();
        entity.localizedName = pokemon.localizedName();
        entity.region = pokemon.region();
        entity.tags = new ArrayList<>(pokemon.tags());
        return entity;
    }

    StoredPokemon toStoredPokemon() {
        return new StoredPokemon(id, name, category, weight, sprite, localizedName, region, List.copyOf(tags));
    }
}
