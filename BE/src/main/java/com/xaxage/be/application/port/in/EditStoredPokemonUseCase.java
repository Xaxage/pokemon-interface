package com.xaxage.be.application.port.in;

import com.xaxage.be.domain.model.StoredPokemon;

import java.util.List;

public interface EditStoredPokemonUseCase {

    /**
     * Replaces our own fields of the stored Pokémon with the id by the ones given; PokeAPI's fields stay as they are.
     *
     * @return the stored Pokémon, as it is after editing
     */
    StoredPokemon edit(int id, String localizedName, String region, List<String> tags);
}
