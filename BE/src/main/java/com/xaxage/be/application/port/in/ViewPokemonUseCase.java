package com.xaxage.be.application.port.in;

import com.xaxage.be.domain.model.DetailedPokemon;

public interface ViewPokemonUseCase {

    DetailedPokemon view(int id);
}
