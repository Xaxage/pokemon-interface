package com.xaxage.be.application.port.in;

import com.xaxage.be.domain.model.StoredPokemonPage;

public interface BrowseStoredPokemonUseCase {

    StoredPokemonPage browse(int page, int size);
}
