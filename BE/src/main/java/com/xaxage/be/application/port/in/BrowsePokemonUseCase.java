package com.xaxage.be.application.port.in;

import com.xaxage.be.domain.model.PokemonPage;

public interface BrowsePokemonUseCase {

    PokemonPage browse(int page, int size);
}
