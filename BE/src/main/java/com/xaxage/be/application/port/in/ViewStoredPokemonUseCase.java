package com.xaxage.be.application.port.in;

import com.xaxage.be.domain.model.StoredPokemon;

public interface ViewStoredPokemonUseCase {

    StoredPokemon view(int id);
}
